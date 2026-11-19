package com.example.room.service;

import com.example.image.service.ImageService;
import com.example.room.dto.request.LayoutJobCreateRequest;
import com.example.room.dto.response.LayoutJobResponse;
import com.example.room.global.client.LayoutClient;
import com.example.room.global.exception.InvalidRoomException;
import com.example.room.global.exception.LayoutBusyException;
import com.example.room.global.exception.LayoutJobNotFoundException;
import com.example.room.global.exception.LayoutRejectedException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 360도 사진 → 방 모서리 추론을 비동기 작업으로 돌린다. 추론이 7~12초 걸려서 요청 하나로 기다리게 하지 않고,
 * 작업 id를 바로 돌려준 뒤 프론트가 상태를 폴링한다.
 *
 * 추론 서비스가 한 번에 하나씩만 처리하므로 이 인스턴스도 작업을 하나씩 보낸다. 대기열이 가득 차면 바로 거절해서
 * 사용자가 수십 초씩 기다리지 않게 한다. 인스턴스가 죽으면 진행 중이던 작업은 RUNNING으로 남다가 TTL로 사라지므로,
 * 프론트는 폴링에 상한 시간을 둔다.
 */
@Slf4j
@Service
public class LayoutJobService {

    static final int MAX_QUEUED_JOBS = 8;
    private static final String GENERIC_FAILURE = "방 구조를 찾지 못했어요. 잠시 후 다시 시도해 주세요.";

    private final LayoutJobStore jobStore;
    private final LayoutClient layoutClient;
    private final ImageService imageService;
    private final ExecutorService executor;

    @Autowired
    public LayoutJobService(LayoutJobStore jobStore, LayoutClient layoutClient, ImageService imageService) {
        this(jobStore, layoutClient, imageService, new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUED_JOBS),
                runnable -> {
                    Thread thread = new Thread(runnable, "room-layout-job");
                    thread.setDaemon(true);
                    return thread;
                }
        ));
    }

    LayoutJobService(LayoutJobStore jobStore, LayoutClient layoutClient, ImageService imageService, ExecutorService executor) {
        this.jobStore = jobStore;
        this.layoutClient = layoutClient;
        this.imageService = imageService;
        this.executor = executor;
    }

    public LayoutJobResponse create(String userId, LayoutJobCreateRequest request) {
        if (!imageService.isOwnImageUrl(request.imageUrl())) {
            throw new InvalidRoomException("이 서비스에 올린 360도 사진만 쓸 수 있습니다.");
        }

        LayoutJob job = LayoutJob.queued(UUID.randomUUID().toString(), userId, System.currentTimeMillis());
        jobStore.save(job);

        try {
            executor.execute(() -> run(job, request));
        } catch (RejectedExecutionException e) {
            jobStore.save(job.failed("요청이 많아요. 잠시 후 다시 시도해 주세요."));
            throw new LayoutBusyException("방 구조 추론 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        }

        return LayoutJobResponse.from(job);
    }

    /** 다른 사람의 작업은 없는 것으로 본다. */
    public LayoutJobResponse get(String userId, String jobId) {
        return jobStore.find(jobId)
                .filter(job -> job.ownerId().equals(userId))
                .map(LayoutJobResponse::from)
                .orElseThrow(() -> new LayoutJobNotFoundException("추론 작업을 찾을 수 없습니다."));
    }

    void run(LayoutJob job, LayoutJobCreateRequest request) {
        LayoutJob running = job.running();
        jobStore.save(running);

        try {
            byte[] panorama = imageService.downloadOwnImage(request.imageUrl());
            LayoutClient.InferResult inferred = layoutClient.infer(
                    panorama, request.alignOrDefault(), request.cuboidOrDefault());

            // uv 좌표는 수평을 맞춘 사진 기준이라, 그 사진을 올려서 화면에서 써야 한다
            String alignedUrl = imageService.uploadGeneratedJpeg(Base64.getDecoder().decode(inferred.image())).imageUrl();

            jobStore.save(running.succeeded(new LayoutJob.Result(
                    alignedUrl, inferred.uv(), inferred.z0(), inferred.z1(), inferred.aligned())));
        } catch (LayoutRejectedException e) {
            log.info("방 구조 추론 거절 (jobId={}): {}", job.id(), e.getMessage());
            jobStore.save(running.failed(e.getMessage()));
        } catch (Exception e) {
            log.error("방 구조 추론 실패 (jobId={})", job.id(), e);
            jobStore.save(running.failed(GENERIC_FAILURE));
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
