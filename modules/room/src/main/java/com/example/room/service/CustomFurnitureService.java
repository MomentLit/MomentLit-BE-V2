package com.example.room.service;

import com.example.image.service.ImageService;
import com.example.room.dto.request.CustomFurnitureFileRequest;
import com.example.room.dto.request.CustomFurniturePhotoRequest;
import com.example.room.dto.request.CustomFurnitureUpdateRequest;
import com.example.room.dto.response.CustomFurnitureResponse;
import com.example.room.entity.CustomFurniture;
import com.example.room.entity.CustomFurnitureSource;
import com.example.room.entity.CustomFurnitureStatus;
import com.example.room.global.client.LayoutClient;
import com.example.room.global.client.LayoutClient.FurnitureResult;
import com.example.room.global.exception.CustomFurnitureNotFoundException;
import com.example.room.global.exception.InvalidRoomException;
import com.example.room.global.exception.LayoutBusyException;
import com.example.room.global.exception.LayoutRejectedException;
import com.example.room.repository.CustomFurnitureRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 내 가구 — 사진으로 생성(RoomScan, 비동기)하거나 GLB 파일을 올린다. 만든 사람만 보고 쓴다.
 *
 * 사진 생성은 CPU로 40초 안팎이고 RoomScan이 한 번에 하나씩 처리해서, 행을 PROCESSING으로 먼저 저장해 돌려주고
 * 이 인스턴스의 단일 작업 스레드가 하나씩 보낸다. 끝나면 READY(모델·썸네일 S3) 또는 FAILED(사유).
 * 인스턴스가 죽어 PROCESSING으로 남은 행은 목록을 볼 때 오래됐으면 FAILED로 바꾼다.
 */
@Slf4j
@Service
public class CustomFurnitureService {

    static final int MAX_PER_USER = 20;
    static final int MAX_PHOTO_PER_DAY = 5;
    static final int MAX_QUEUED_JOBS = 8;
    static final long MAX_PHOTO_BYTES = 20L * 1024 * 1024;
    static final long MAX_MODEL_BYTES = 30L * 1024 * 1024;
    static final long MAX_THUMBNAIL_BYTES = 2L * 1024 * 1024;
    static final Duration STALE_AFTER = Duration.ofMinutes(15);

    private static final Set<String> PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String GENERIC_FAILURE = "3D 모델을 만들지 못했어요. 잠시 후 다시 시도해 주세요.";
    private static final String STALE_FAILURE = "만드는 데 너무 오래 걸려 멈췄어요. 다시 만들어 주세요.";
    private static final String BUSY_FAILURE = "요청이 많아요. 잠시 후 다시 시도해 주세요.";

    private final CustomFurnitureRepository repository;
    private final LayoutClient layoutClient;
    private final ImageService imageService;
    private final ExecutorService executor;

    @Autowired
    public CustomFurnitureService(CustomFurnitureRepository repository, LayoutClient layoutClient, ImageService imageService) {
        this(repository, layoutClient, imageService, new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUED_JOBS),
                runnable -> {
                    Thread thread = new Thread(runnable, "room-furniture-job");
                    thread.setDaemon(true);
                    return thread;
                }
        ));
    }

    CustomFurnitureService(CustomFurnitureRepository repository, LayoutClient layoutClient, ImageService imageService, ExecutorService executor) {
        this.repository = repository;
        this.layoutClient = layoutClient;
        this.imageService = imageService;
        this.executor = executor;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    @Transactional
    public List<CustomFurnitureResponse> getMine(String userId) {
        LocalDateTime staleBefore = LocalDateTime.now().minus(STALE_AFTER);
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .peek(furniture -> {
                    if (furniture.getStatus() == CustomFurnitureStatus.PROCESSING && furniture.getCreatedAt().isBefore(staleBefore)) {
                        furniture.fail(STALE_FAILURE);
                    }
                })
                .map(CustomFurnitureResponse::from)
                .toList();
    }

    public CustomFurnitureResponse get(String userId, Long id) {
        return CustomFurnitureResponse.from(find(userId, id));
    }

    public byte[] downloadAsset(String userId, Long id, boolean thumbnail) {
        CustomFurniture furniture = find(userId, id);
        String key = thumbnail ? furniture.getThumbnailUrl() : furniture.getModelUrl();
        byte[] bytes = furniture.getStatus() == CustomFurnitureStatus.READY && key != null
                ? imageService.downloadFurnitureFile(key) : null;
        if (bytes == null) {
            throw new CustomFurnitureNotFoundException("내 가구 파일을 찾을 수 없습니다.");
        }
        return bytes;
    }

    public CustomFurnitureResponse createFromPhoto(String userId, CustomFurniturePhotoRequest request, MultipartFile front, MultipartFile side) {
        byte[] frontBytes = readPhoto(front, "정면");
        byte[] sideBytes = side == null || side.isEmpty() ? null : readPhoto(side, "옆");
        checkCount(userId);
        long today = repository.countByUserIdAndSourceAndCreatedAtAfter(userId, CustomFurnitureSource.PHOTO, LocalDate.now().atStartOfDay());
        if (today >= MAX_PHOTO_PER_DAY) {
            throw new InvalidRoomException("사진으로 만들기는 하루 " + MAX_PHOTO_PER_DAY + "번까지 할 수 있어요.");
        }

        CustomFurniture furniture = repository.save(CustomFurniture.processing(
                userId, request.name(), request.mount(), request.height(), request.width(), request.depth()));
        Long id = furniture.getId();
        try {
            executor.execute(() -> generate(id, frontBytes, sideBytes, request));
        } catch (RejectedExecutionException e) {
            furniture.fail(BUSY_FAILURE);
            repository.save(furniture);
            throw new LayoutBusyException("가구 생성 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        }
        return CustomFurnitureResponse.from(furniture);
    }

    public CustomFurnitureResponse createFromFile(String userId, CustomFurnitureFileRequest request, MultipartFile model, MultipartFile thumbnail) {
        byte[] glb = readFile(model, MAX_MODEL_BYTES, "3D 파일은 30MB까지 올릴 수 있어요.");
        validateGlb(glb);
        byte[] png = readFile(thumbnail, MAX_THUMBNAIL_BYTES, "썸네일은 2MB까지 올릴 수 있어요.");
        if (!isPng(png)) {
            throw new InvalidRoomException("썸네일은 PNG여야 합니다.");
        }
        checkCount(userId);

        String modelUrl = imageService.uploadModel(glb).imageUrl();
        String thumbnailUrl = imageService.uploadGeneratedPng(png).imageUrl();
        CustomFurniture furniture = repository.save(CustomFurniture.uploaded(
                userId, request.name(), request.mount(), request.width(), request.depth(), request.height(), modelUrl, thumbnailUrl));
        return CustomFurnitureResponse.from(furniture);
    }

    @Transactional
    public CustomFurnitureResponse update(String userId, Long id, CustomFurnitureUpdateRequest request) {
        CustomFurniture furniture = find(userId, id);
        furniture.update(request.name(), request.mount());
        return CustomFurnitureResponse.from(furniture);
    }

    public void delete(String userId, Long id) {
        CustomFurniture furniture = find(userId, id);
        repository.delete(furniture);
        deleteFiles(furniture.getModelUrl(), furniture.getThumbnailUrl());
    }

    /** 작업 스레드: RoomScan으로 생성 → S3 → READY. 그 사이 지워졌으면 올린 파일도 지운다. */
    void generate(Long id, byte[] front, byte[] side, CustomFurniturePhotoRequest request) {
        try {
            FurnitureResult result = layoutClient.generateFurniture(front, side, request.height(), request.width(), request.depth());
            String modelUrl = imageService.uploadModel(Base64.getDecoder().decode(result.glb())).imageUrl();
            String thumbnailUrl = imageService.uploadGeneratedPng(Base64.getDecoder().decode(result.thumbnail())).imageUrl();
            repository.findById(id).ifPresentOrElse(furniture -> {
                furniture.ready(result.width(), result.depth(), result.height(), modelUrl, thumbnailUrl);
                repository.save(furniture);
            }, () -> deleteFiles(modelUrl, thumbnailUrl));
        } catch (LayoutRejectedException e) {
            markFailed(id, e.getMessage());
        } catch (RuntimeException e) {
            log.error("내 가구 생성 실패 id={}", id, e);
            markFailed(id, GENERIC_FAILURE);
        }
    }

    private void markFailed(Long id, String reason) {
        repository.findById(id).ifPresent(furniture -> {
            furniture.fail(reason);
            repository.save(furniture);
        });
    }

    private void deleteFiles(String... urls) {
        for (String url : urls) {
            if (url == null) continue;
            try {
                imageService.deleteOwnFile(url);
            } catch (RuntimeException e) {
                log.warn("내 가구 파일 삭제 실패 {}: {}", url, e.getMessage());
            }
        }
    }

    private CustomFurniture find(String userId, Long id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomFurnitureNotFoundException("내 가구를 찾을 수 없습니다."));
    }

    private void checkCount(String userId) {
        if (repository.countByUserId(userId) >= MAX_PER_USER) {
            throw new InvalidRoomException("내 가구는 " + MAX_PER_USER + "개까지 만들 수 있어요. 안 쓰는 가구를 지워 주세요.");
        }
    }

    private byte[] readPhoto(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRoomException(label + " 사진을 올려 주세요.");
        }
        if (!PHOTO_TYPES.contains(file.getContentType())) {
            throw new InvalidRoomException("사진은 jpeg/png/webp만 올릴 수 있어요.");
        }
        return readFile(file, MAX_PHOTO_BYTES, "사진은 20MB까지 올릴 수 있어요.");
    }

    private byte[] readFile(MultipartFile file, long maxBytes, String tooLarge) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRoomException("파일을 올려 주세요.");
        }
        if (file.getSize() > maxBytes) {
            throw new InvalidRoomException(tooLarge);
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new InvalidRoomException("파일을 읽지 못했어요.");
        }
    }

    /** GLB 2.0 머리글: "glTF" + 버전 2 + 전체 길이가 실제 길이와 같아야 한다. */
    static void validateGlb(byte[] glb) {
        if (glb.length < 20) {
            throw new InvalidRoomException("GLB 파일이 아니에요.");
        }
        ByteBuffer header = ByteBuffer.wrap(glb, 0, 12).order(ByteOrder.LITTLE_ENDIAN);
        int magic = header.getInt();
        int version = header.getInt();
        int length = header.getInt();
        if (magic != 0x46546C67) {
            throw new InvalidRoomException("GLB 파일이 아니에요.");
        }
        if (version != 2) {
            throw new InvalidRoomException("glTF 2.0 GLB만 올릴 수 있어요.");
        }
        if (length != glb.length) {
            throw new InvalidRoomException("GLB 파일이 깨졌어요.");
        }
    }

    private static boolean isPng(byte[] bytes) {
        return bytes.length > 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
    }
}
