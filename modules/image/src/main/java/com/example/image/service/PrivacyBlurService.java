package com.example.image.service;

import static org.bytedeco.opencv.global.opencv_core.hconcat;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.bytedeco.opencv.global.opencv_imgproc.GaussianBlur;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_AREA;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_LINEAR;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

import com.example.image.global.client.LocalDetectionClient;
import com.example.image.global.client.LocalDetectionClient.DetectedRegion;
import com.example.image.global.exception.PrivacyBlurFailedException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.stereotype.Service;

/**
 * 업로드 사진 속 사람 얼굴·차량 번호판 자동 블라인드.
 * 로컬 AI 모델(LocalDetectionClient)로 위치만 찾고, 찾은 영역을 OpenCV 가우시안 흐림으로 가린다.
 * 검출에 실패하면(모델 로드 실패·처리 오류 등) 블라인드 없이 원본을 그대로 돌려준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrivacyBlurService {

    private static final int NORMALIZED_MAX = 1000;

    // 검출 영역을 가로·세로로 각각 10%씩 넓혀서 가린다(가장자리가 삐져나오지 않도록).
    private static final double REGION_PADDING_RATIO = 0.1;

    // 검출 모델에 넘기는 이미지의 긴 변 최대 길이 — 좌표는 0~1000 비율이라 줄여 넘겨도 그대로 쓸 수 있다.
    private static final int DETECTION_MAX_SIDE = 2048;

    // 360도 사진은 가로로 길어서 한 장으로 보내면 얼굴이 너무 작아진다 — 가로로 나눠 각각 검출한다.
    private static final int PANORAMA_TILE_COUNT = 4;

    private static final double PANORAMA_TILE_OVERLAP_RATIO = 0.1;

    // 흐림 강도 — 영역을 이 크기까지 줄여 흐린 뒤 다시 키운다(큰 영역도 알아볼 수 없을 만큼 흐려짐).
    private static final int BLUR_WORK_SIDE = 32;

    private static final double BLUR_SIGMA = 3.0;

    private final LocalDetectionClient localDetectionClient;

    /**
     * @param extension 다시 인코딩할 형식(".jpg" / ".png" / ".webp")
     * @param panorama  360도 사진이면 true — 가로 분할 + 좌우 이음새 검출
     * @return 블라인드 처리된 이미지. 가릴 영역이 없거나 처리에 실패하면 원본 그대로.
     */
    public byte[] blur(byte[] bytes, String extension, boolean panorama) {
        try (PointerScope scope = new PointerScope()) {
            Mat image = imdecode(new Mat(bytes), IMREAD_COLOR);
            if (image.empty()) {
                log.warn("블라인드 처리할 이미지를 읽을 수 없어 원본을 그대로 업로드합니다.");
                return bytes;
            }

            List<Tile> tiles = panorama ? createPanoramaTiles(image) : List.of(Tile.whole(image));
            List<PixelRegion> regions = detectRegions(image, tiles);

            if (regions.isEmpty()) {
                return bytes;
            }

            regions.forEach(region -> blurRegion(image, region));

            BytePointer buffer = new BytePointer();
            if (!imencode(extension, image, buffer)) {
                log.warn("블라인드 처리 결과를 인코딩하지 못해 원본을 그대로 업로드합니다. (extension={})", extension);
                return bytes;
            }

            byte[] blurred = new byte[(int) buffer.limit()];
            buffer.get(blurred);

            log.info("사진 블라인드 처리 완료 (regions={}, panorama={})", regions.size(), panorama);
            return blurred;
        } catch (PrivacyBlurFailedException e) {
            log.warn("사진 블라인드 처리 실패, 원본을 그대로 업로드합니다: {}", e.getMessage());
            return bytes;
        } catch (Exception | LinkageError e) {
            log.warn("사진 블라인드 처리 중 오류, 원본을 그대로 업로드합니다.", e);
            return bytes;
        }
    }

    /**
     * 360도 사진: 겹치게 나눈 가로 조각 4개 + 좌우 끝을 이어 붙인 이음새 조각 1개.
     * 파노라마의 오른쪽 끝과 왼쪽 끝은 실제로 이어진 장면이라, 경계에 걸친 얼굴을 놓치지 않게 따로 본다.
     */
    private List<Tile> createPanoramaTiles(Mat image) {
        int width = image.cols();
        int height = image.rows();
        int tileWidth = width / PANORAMA_TILE_COUNT;
        int overlap = (int) (tileWidth * PANORAMA_TILE_OVERLAP_RATIO);

        List<Tile> tiles = new ArrayList<>();
        for (int i = 0; i < PANORAMA_TILE_COUNT; i++) {
            int left = Math.max(0, i * tileWidth - overlap);
            int right = i == PANORAMA_TILE_COUNT - 1 ? width : Math.min(width, (i + 1) * tileWidth + overlap);
            Mat crop = new Mat(image, new Rect(left, 0, right - left, height));
            tiles.add(new Tile(crop, left, 0));
        }

        int seamHalf = tileWidth / 2;
        Mat rightEdge = new Mat(image, new Rect(width - seamHalf, 0, seamHalf, height));
        Mat leftEdge = new Mat(image, new Rect(0, 0, seamHalf, height));
        Mat seam = new Mat();
        hconcat(rightEdge, leftEdge, seam);
        tiles.add(Tile.seam(seam, width - seamHalf));

        return tiles;
    }

    /** 조각별 인코딩은 이 스레드에서 하고(네이티브 메모리 관리), 검출만 병렬로 돌린다. */
    private List<PixelRegion> detectRegions(Mat image, List<Tile> tiles) {
        List<byte[]> tileImages = tiles.stream()
                .map(tile -> encodeForDetection(tile.image()))
                .toList();

        List<List<DetectedRegion>> detections = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<DetectedRegion>>> futures = tileImages.stream()
                    .map(tileImage -> executor.submit(() -> localDetectionClient.detect(tileImage)))
                    .toList();

            for (Future<List<DetectedRegion>> future : futures) {
                detections.add(future.get());
            }
        } catch (ExecutionException e) {
            if (e.getCause() instanceof PrivacyBlurFailedException cause) {
                throw cause;
            }
            throw new PrivacyBlurFailedException("얼굴·번호판 검출 중 오류가 발생했습니다.", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PrivacyBlurFailedException("얼굴·번호판 검출이 중단되었습니다.", e);
        }

        List<PixelRegion> regions = new ArrayList<>();
        for (int i = 0; i < tiles.size(); i++) {
            Tile tile = tiles.get(i);
            for (DetectedRegion detected : detections.get(i)) {
                regions.addAll(toImageRegions(image, tile, detected));
            }
        }
        return regions;
    }

    private byte[] encodeForDetection(Mat tileImage) {
        Mat target = tileImage;
        int longSide = Math.max(tileImage.cols(), tileImage.rows());
        if (longSide > DETECTION_MAX_SIDE) {
            double scale = (double) DETECTION_MAX_SIDE / longSide;
            target = new Mat();
            resize(tileImage, target, new Size(
                    Math.max(1, (int) Math.round(tileImage.cols() * scale)),
                    Math.max(1, (int) Math.round(tileImage.rows() * scale))
            ), 0, 0, INTER_AREA);
        }

        BytePointer buffer = new BytePointer();
        if (!imencode(".jpg", target, buffer)) {
            throw new PrivacyBlurFailedException("검출용 이미지를 만들지 못했습니다.");
        }

        byte[] bytes = new byte[(int) buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }

    /** 조각 기준 0~1000 좌표 → 여백 10% 추가 → 원본 이미지 픽셀 좌표. 이음새 조각은 좌우 두 영역으로 나뉠 수 있다. */
    private List<PixelRegion> toImageRegions(Mat image, Tile tile, DetectedRegion detected) {
        int tileWidth = tile.image().cols();
        int tileHeight = tile.image().rows();

        int left = detected.xMin() * tileWidth / NORMALIZED_MAX;
        int right = detected.xMax() * tileWidth / NORMALIZED_MAX;
        int top = detected.yMin() * tileHeight / NORMALIZED_MAX;
        int bottom = detected.yMax() * tileHeight / NORMALIZED_MAX;

        if (right <= left || bottom <= top) {
            log.warn("잘못된 검출 좌표를 무시합니다: {}", detected);
            return List.of();
        }

        int padX = (int) Math.ceil((right - left) * REGION_PADDING_RATIO);
        int padY = (int) Math.ceil((bottom - top) * REGION_PADDING_RATIO);
        left = Math.max(0, left - padX);
        right = Math.min(tileWidth, right + padX);
        top = Math.max(0, top - padY);
        bottom = Math.min(tileHeight, bottom + padY);

        int imageWidth = image.cols();
        int imageTop = tile.offsetY() + top;
        int imageBottom = tile.offsetY() + bottom;

        if (!tile.seam()) {
            return List.of(new PixelRegion(tile.offsetX() + left, imageTop, tile.offsetX() + right, imageBottom));
        }

        // 이음새 조각: [0, seamHalf)는 원본의 오른쪽 끝, [seamHalf, tileWidth)는 원본의 왼쪽 끝
        int seamHalf = imageWidth - tile.offsetX();
        List<PixelRegion> regions = new ArrayList<>();
        if (left < seamHalf) {
            regions.add(new PixelRegion(tile.offsetX() + left, imageTop, tile.offsetX() + Math.min(right, seamHalf), imageBottom));
        }
        if (right > seamHalf) {
            regions.add(new PixelRegion(Math.max(left, seamHalf) - seamHalf, imageTop, right - seamHalf, imageBottom));
        }
        return regions;
    }

    /** 영역을 작게 줄여 가우시안 흐림 후 원래 크기로 되돌린다 — 영역 크기와 상관없이 알아볼 수 없을 만큼 흐려진다. */
    private void blurRegion(Mat image, PixelRegion region) {
        int left = Math.max(0, region.left());
        int top = Math.max(0, region.top());
        int right = Math.min(image.cols(), region.right());
        int bottom = Math.min(image.rows(), region.bottom());
        int width = right - left;
        int height = bottom - top;

        if (width <= 0 || height <= 0) {
            return;
        }

        Mat roi = new Mat(image, new Rect(left, top, width, height));

        double scale = Math.min(1.0, (double) BLUR_WORK_SIDE / Math.max(width, height));
        Mat small = new Mat();
        resize(roi, small, new Size(
                Math.max(1, (int) Math.round(width * scale)),
                Math.max(1, (int) Math.round(height * scale))
        ), 0, 0, INTER_AREA);
        GaussianBlur(small, small, new Size(0, 0), BLUR_SIGMA);

        Mat blurred = new Mat();
        resize(small, blurred, new Size(width, height), 0, 0, INTER_LINEAR);
        blurred.copyTo(roi);
    }

    private record Tile(
            Mat image,
            int offsetX,
            int offsetY,
            boolean seam
    ) {
        Tile(Mat image, int offsetX, int offsetY) {
            this(image, offsetX, offsetY, false);
        }

        static Tile whole(Mat image) {
            return new Tile(image, 0, 0, false);
        }

        static Tile seam(Mat image, int offsetX) {
            return new Tile(image, offsetX, 0, true);
        }
    }

    private record PixelRegion(
            int left,
            int top,
            int right,
            int bottom
    ) {
    }
}
