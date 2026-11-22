package com.example.image.service;

import com.example.image.dto.response.ImageUploadResponse;
import com.example.image.global.client.PanoramaStitchClient;
import com.example.image.global.client.PanoramaStitchClient.StitchedPanorama;
import com.example.image.global.exception.EmptyImageFileException;
import com.example.image.global.exception.ImageUploadFailedException;
import com.example.image.global.exception.InvalidImageTypeException;
import com.example.image.global.exception.InvalidImageUrlException;
import com.example.image.global.exception.InvalidPanoramaRatioException;
import com.example.image.global.exception.PanoramaImageCountException;
import com.example.image.global.exception.PanoramaStitchFailedException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final String IMAGE_DIRECTORY = "images";

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    // 비율 검증에 ImageIO를 쓰는데, JDK 기본 ImageIO에는 webp 리더가 없어서 360도 사진은 jpeg/png만 받는다.
    private static final Set<String> PANORAMA_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );

    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    // 등장방형(equirectangular) 360도 사진은 가로:세로 = 2:1
    private static final double PANORAMA_RATIO = 2.0;

    // 카메라·앱마다 픽셀 수가 조금씩 달라(예: 1456×720 = 2.022) 너무 좁으면 멀쩡한 360도 사진이 막힌다.
    // 뷰어와 방 구조 추론은 어차피 2:1로 늘려 쓰므로 ±0.05(약 2.5%)까지는 같은 사진으로 본다. 프론트와 같은 값.
    private static final double PANORAMA_RATIO_TOLERANCE = 0.05;

    // 수평 45도 간격 8장 + 천장 1장 + 바닥 1장
    private static final int PANORAMA_SOURCE_COUNT = 10;

    private final S3Client s3Client;

    private final PanoramaStitchClient panoramaStitchClient;

    private final PrivacyBlurService privacyBlurService;

    @Value("${cloud.aws.region}")
    private String region;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.private-furniture-bucket}")
    private String privateFurnitureBucket;

    /**
     * @param privacyBlur false면 얼굴·번호판 자동 블라인드를 건너뛴다 — 본인 얼굴이 주인공인 프로필 사진용.
     */
    public ImageUploadResponse upload(MultipartFile file, boolean privacyBlur) {
        validateImage(file);

        byte[] bytes = readBytes(file);
        if (privacyBlur) {
            bytes = privacyBlurService.blur(bytes, CONTENT_TYPE_EXTENSIONS.get(file.getContentType()), false);
        }

        return putImage(bytes, file.getContentType(), extractExtension(file.getOriginalFilename()));
    }

    public ImageUploadResponse uploadPanorama(MultipartFile file) {
        validateImage(file);
        validatePanoramaType(file);
        validatePanoramaRatio(file);

        byte[] bytes = privacyBlurService.blur(readBytes(file), CONTENT_TYPE_EXTENSIONS.get(file.getContentType()), true);

        return putImage(bytes, file.getContentType(), extractExtension(file.getOriginalFilename()));
    }

    public ImageUploadResponse stitchPanorama(List<MultipartFile> files) {
        if (files == null || files.size() != PANORAMA_SOURCE_COUNT) {
            throw new PanoramaImageCountException("360도 사진 합성에는 사진 " + PANORAMA_SOURCE_COUNT + "장이 필요합니다.");
        }

        files.forEach(this::validateImage);

        StitchedPanorama stitched = panoramaStitchClient.stitch(files);

        String extension = CONTENT_TYPE_EXTENSIONS.get(stitched.contentType());
        if (extension == null) {
            throw new PanoramaStitchFailedException("360도 사진 합성 결과의 이미지 형식이 올바르지 않습니다.");
        }
        validateStitchedPanoramaRatio(stitched.bytes());

        byte[] bytes = privacyBlurService.blur(stitched.bytes(), extension, true);

        return putImage(bytes, stitched.contentType(), extension);
    }

    /** 이 서비스가 올린 이미지 URL인지 — 버킷 주소 + images/ 아래 키만 인정한다. */
    public boolean isOwnImageUrl(String imageUrl) {
        return extractOwnImageKey(imageUrl) != null;
    }

    /**
     * 우리 버킷에 올라간 이미지를 내려받는다. 다른 서비스(방 구조 추론 등)에 넘기기 위한 것이라
     * HTTP로 가져오지 않고 S3에서 키로 직접 읽는다 — 임의 URL을 서버가 대신 요청하지 않도록.
     */
    public byte[] downloadOwnImage(String imageUrl) {
        String key = extractOwnImageKey(imageUrl);
        if (key == null) {
            throw new InvalidImageUrlException("이 서비스에 올린 이미지 URL이 아닙니다.");
        }
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new InvalidImageUrlException("이미지를 찾을 수 없습니다.");
        }
    }

    /** 서버가 만든 JPEG(예: 수평을 맞춘 파노라마)를 그대로 올린다. 원본이 이미 블라인드 처리된 사진이라 다시 하지 않는다. */
    public ImageUploadResponse uploadGeneratedJpeg(byte[] bytes) {
        return putImage(bytes, "image/jpeg", ".jpg");
    }

    /** 서버가 만든 PNG(예: 배경을 지운 가구 썸네일)를 올린다. */
    public ImageUploadResponse uploadGeneratedPng(byte[] bytes) {
        return putFurnitureFile(bytes, "image/png", ".png");
    }

    /** 3D 가구 모델(GLB)을 올린다. 형식 검사는 호출하는 쪽이 한다. */
    public ImageUploadResponse uploadModel(byte[] glb) {
        return putFurnitureFile(glb, "model/gltf-binary", ".glb");
    }

    public byte[] downloadFurnitureFile(String key) {
        if (!isFurnitureKey(key)) return null;
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(furnitureBucket()).key(key).build()).asByteArray();
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    public void deleteOwnFile(String key) {
        if (isFurnitureKey(key)) {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(furnitureBucket()).key(key).build());
        }
    }

    private boolean isFurnitureKey(String key) {
        return key != null && key.matches("furniture/[a-f0-9-]{36}\\.(glb|png)");
    }

    private ImageUploadResponse putFurnitureFile(byte[] bytes, String contentType, String extension) {
        String key = "furniture/" + UUID.randomUUID() + extension;
        s3Client.putObject(PutObjectRequest.builder().bucket(furnitureBucket()).key(key)
                .contentType(contentType).contentLength((long) bytes.length).build(), RequestBody.fromBytes(bytes));
        return new ImageUploadResponse(key);
    }

    private String furnitureBucket() {
        if (privateFurnitureBucket.isBlank() || privateFurnitureBucket.equals(bucket)) {
            throw new IllegalStateException("A separate private furniture bucket is required");
        }
        return privateFurnitureBucket;
    }

    private String extractOwnImageKey(String imageUrl) {
        String prefix = createImageUrl(IMAGE_DIRECTORY + "/");
        if (imageUrl == null || !imageUrl.startsWith(prefix)) {
            return null;
        }
        String name = imageUrl.substring(prefix.length());
        // putImage가 만드는 키는 images/{UUID}{확장자} 한 단계뿐이다
        if (name.isEmpty() || !name.matches("[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)?")) {
            return null;
        }
        return IMAGE_DIRECTORY + "/" + name;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ImageUploadFailedException("이미지 업로드 중 오류가 발생했습니다.", e);
        }
    }

    private ImageUploadResponse putImage(byte[] bytes, String contentType, String extension) {
        String key = IMAGE_DIRECTORY + "/" + UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength((long) bytes.length)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));

        return new ImageUploadResponse(createImageUrl(key));
    }

    private void validatePanoramaType(MultipartFile file) {
        if (!PANORAMA_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidImageTypeException("360도 사진은 jpeg/png 형식만 지원합니다.");
        }
    }

    private void validatePanoramaRatio(MultipartFile file) {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) {
                throw new InvalidPanoramaRatioException("이미지 크기를 확인할 수 없습니다.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidPanoramaRatioException("이미지 크기를 확인할 수 없습니다.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                double ratio = (double) reader.getWidth(0) / reader.getHeight(0);

                if (Math.abs(ratio - PANORAMA_RATIO) > PANORAMA_RATIO_TOLERANCE) {
                    throw new InvalidPanoramaRatioException("360도 사진은 가로:세로 비율이 2:1이어야 합니다.");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new ImageUploadFailedException("이미지 크기 확인 중 오류가 발생했습니다.", e);
        }
    }

    // 합성 결과도 직접 올린 360도 사진과 같은 2:1 비율이어야 뷰어에서 늘어나거나 어긋나 보이지 않는다.
    private void validateStitchedPanoramaRatio(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new PanoramaStitchFailedException("360도 사진 합성 결과의 이미지 형식이 올바르지 않습니다.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new PanoramaStitchFailedException("360도 사진 합성 결과의 이미지 형식이 올바르지 않습니다.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                double ratio = (double) reader.getWidth(0) / reader.getHeight(0);

                if (Math.abs(ratio - PANORAMA_RATIO) > PANORAMA_RATIO_TOLERANCE) {
                    throw new PanoramaStitchFailedException("360도 사진 합성 결과의 가로:세로 비율이 2:1이 아닙니다.");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new PanoramaStitchFailedException("360도 사진 합성 결과를 확인하는 중 오류가 발생했습니다.", e);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new EmptyImageFileException("이미지 파일이 비어 있습니다.");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidImageTypeException("지원하지 않는 이미지 형식입니다.");
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }

        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    private String createImageUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}
