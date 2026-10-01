package com.example.image.service;

import com.example.image.dto.response.ImageUploadResponse;
import com.example.image.global.client.PanoramaStitchClient;
import com.example.image.global.client.PanoramaStitchClient.StitchedPanorama;
import com.example.image.global.exception.EmptyImageFileException;
import com.example.image.global.exception.ImageUploadFailedException;
import com.example.image.global.exception.InvalidImageTypeException;
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

    private static final double PANORAMA_RATIO_TOLERANCE = 0.02;

    // 수평 45도 간격 8장 + 천장 1장 + 바닥 1장
    private static final int PANORAMA_SOURCE_COUNT = 10;

    private final S3Client s3Client;

    private final PanoramaStitchClient panoramaStitchClient;

    private final PrivacyBlurService privacyBlurService;

    @Value("${cloud.aws.region}")
    private String region;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

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
