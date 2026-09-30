package com.example.image.global.client;

import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;

import com.example.image.global.exception.PanoramaStitchFailedException;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_stitching.Stitcher;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 360도 사진 합성 — OpenCV Stitcher(PANORAMA 모드)를 백엔드 프로세스 안에서 실행한다.
 * https://docs.opencv.org/4.13.0/d8/d19/tutorial_stitcher.html
 * 결과는 사진들이 실제로 덮은 영역만큼의 구면 파노라마라서 정확한 2:1이 아닐 수 있다. 뷰어는 이를
 * 위아래가 잘린 파노라마로 보여준다.
 */
@Slf4j
@Component
public class PanoramaStitchClient {

    // 프론트 PANORAMA_CAPTURE_STEPS 순서: 수평 8장 → 천장 → 바닥
    private static final int HORIZONTAL_PHOTO_COUNT = 8;

    private static final String OUTPUT_CONTENT_TYPE = "image/jpeg";

    public StitchedPanorama stitch(List<MultipartFile> files) {
        List<byte[]> images = files.stream()
                .map(this::readBytes)
                .toList();

        try (PointerScope scope = new PointerScope()) {
            Mat panorama = stitchImages(images);

            // 천장·바닥 사진은 특징점이 적어 전체 합성을 실패시키는 경우가 많아, 수평 사진만으로 한 번 더 시도한다.
            if (panorama == null && images.size() > HORIZONTAL_PHOTO_COUNT) {
                panorama = stitchImages(images.subList(0, HORIZONTAL_PHOTO_COUNT));
            }

            if (panorama == null) {
                throw new PanoramaStitchFailedException(
                        "사진끼리 겹치는 부분을 찾지 못했습니다. 가이드대로 이웃한 사진이 1/3 정도 겹치게 다시 찍어 주세요."
                );
            }

            BytePointer buffer = new BytePointer();
            if (!imencode(".jpg", panorama, buffer)) {
                throw new PanoramaStitchFailedException("360도 사진 합성 결과를 저장하지 못했습니다.");
            }

            byte[] bytes = new byte[(int) buffer.limit()];
            buffer.get(bytes);

            return new StitchedPanorama(bytes, OUTPUT_CONTENT_TYPE);
        } catch (PanoramaStitchFailedException e) {
            throw e;
        } catch (Exception | LinkageError e) {
            // LinkageError: 이 플랫폼용 OpenCV 네이티브 라이브러리를 찾지/불러오지 못한 경우
            throw new PanoramaStitchFailedException("360도 사진 합성 중 오류가 발생했습니다.", e);
        }
    }

    /** 합성에 실패하면 null. */
    private Mat stitchImages(List<byte[]> images) {
        MatVector mats = new MatVector(images.size());

        for (int i = 0; i < images.size(); i++) {
            Mat mat = imdecode(new Mat(images.get(i)), IMREAD_COLOR);
            if (mat.empty()) {
                throw new PanoramaStitchFailedException("사진을 읽을 수 없습니다.");
            }
            mats.put(i, mat);
        }

        Stitcher stitcher = Stitcher.create(Stitcher.PANORAMA);
        Mat panorama = new Mat();
        int status;
        try {
            status = stitcher.stitch(mats, panorama);
        } catch (RuntimeException e) {
            // 특징점이 거의 없는 사진(민무늬 천장·바닥 등)이 섞이면 OpenCV가 상태 코드 대신 예외를 던진다.
            log.warn("OpenCV 360도 사진 합성 실패 (images={}): {}", images.size(), e.getMessage());
            return null;
        }

        if (status != Stitcher.OK) {
            log.warn("OpenCV 360도 사진 합성 실패 (images={}, status={})", images.size(), status);
            return null;
        }

        return panorama;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new PanoramaStitchFailedException("사진을 읽는 중 오류가 발생했습니다.", e);
        }
    }

    public record StitchedPanorama(
            byte[] bytes,
            String contentType
    ) {
    }
}
