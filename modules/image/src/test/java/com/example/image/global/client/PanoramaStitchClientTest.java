package com.example.image.global.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.bytedeco.opencv.global.opencv_core.BORDER_WRAP;
import static org.bytedeco.opencv.global.opencv_core.CV_32FC1;
import static org.bytedeco.opencv.global.opencv_core.CV_64F;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC3;
import static org.bytedeco.opencv.global.opencv_core.countNonZero;
import static org.bytedeco.opencv.global.opencv_core.minMaxLoc;
import static org.bytedeco.opencv.global.opencv_core.addWeighted;
import static org.bytedeco.opencv.global.opencv_core.randu;
import static org.bytedeco.opencv.global.opencv_core.theRNG;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.GaussianBlur;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_AREA;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_LINEAR;
import static org.bytedeco.opencv.global.opencv_imgproc.THRESH_BINARY_INV;
import static org.bytedeco.opencv.global.opencv_imgproc.TM_CCOEFF_NORMED;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.matchTemplate;
import static org.bytedeco.opencv.global.opencv_imgproc.remap;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_imgproc.threshold;

import com.example.image.global.client.PanoramaStitchClient.StitchedPanorama;
import com.example.image.global.exception.PanoramaStitchFailedException;
import java.util.ArrayList;
import java.util.List;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class PanoramaStitchClientTest {

    // 합성할 가상 공간 — 등장방형(2:1) 무늬 이미지. 이걸 가이드대로 "촬영"한 사진 10장을 만들어 합성해 본다.
    private static final int SCENE_HEIGHT = 1024;

    private static final int PHOTO_WIDTH = 800;

    private static final int PHOTO_HEIGHT = 600;

    // 휴대폰 기본 카메라를 가로로 든 화각
    private static final double PHOTO_HORIZONTAL_FOV_DEGREES = 65;

    private static Mat scene;

    private final PanoramaStitchClient panoramaStitchClient = new PanoramaStitchClient();

    @BeforeAll
    static void setUpScene() {
        scene = randomTexture(SCENE_HEIGHT * 2, SCENE_HEIGHT, 1);
    }

    @Test
    void stitch_returnsTwoToOnePanoramaWithoutBlankArea() {
        StitchedPanorama result = panoramaStitchClient.stitch(guidedPhotos(scene));

        Mat panorama = imdecode(new Mat(result.bytes()), IMREAD_COLOR);
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(panorama.cols()).isEqualTo(panorama.rows() * 2);

        // 사진이 덮지 못한 곳도 주변 색으로 채워서 검은 빈틈이 남지 않는다.
        Mat gray = new Mat();
        cvtColor(panorama, gray, COLOR_BGR2GRAY);
        Mat black = new Mat();
        threshold(gray, black, 8, 255, THRESH_BINARY_INV);
        assertThat((double) countNonZero(black) / panorama.total()).isLessThan(0.001);
    }

    @Test
    void stitch_placesFrontPhotoAtCenter() {
        StitchedPanorama result = panoramaStitchClient.stitch(guidedPhotos(scene));

        Mat panorama = imdecode(new Mat(result.bytes()), IMREAD_COLOR);
        Mat resized = new Mat();
        resize(panorama, resized, scene.size(), 0, 0, INTER_AREA);

        // 가상 공간의 정면(가운데) 무늬가 결과에서도 가운데에 있어야 한다.
        int patchWidth = SCENE_HEIGHT / 4;
        int patchHeight = SCENE_HEIGHT / 8;
        Rect center = new Rect(SCENE_HEIGHT - patchWidth / 2, SCENE_HEIGHT / 2 - patchHeight / 2, patchWidth, patchHeight);
        Mat matches = new Mat();
        matchTemplate(resized, new Mat(scene, center), matches, TM_CCOEFF_NORMED);

        DoublePointer maxValue = new DoublePointer(1);
        Point maxLocation = new Point();
        minMaxLoc(matches, null, maxValue, null, maxLocation, null);

        assertThat(maxValue.get()).isGreaterThan(0.5);
        assertThat(Math.abs(maxLocation.x() - center.x())).isLessThan(SCENE_HEIGHT * 2 / 50);
        assertThat(Math.abs(maxLocation.y() - center.y())).isLessThan(SCENE_HEIGHT / 25);
    }

    @Test
    void stitch_throws_whenHorizontalPhotosDoNotOverlap() {
        // 수평 사진마다 서로 다른 공간을 찍은 것처럼 만든다.
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            files.add(jpeg(photo(randomTexture(SCENE_HEIGHT * 2, SCENE_HEIGHT, i + 100), 0, 0), "photo" + i + ".jpg"));
        }

        assertThatThrownBy(() -> panoramaStitchClient.stitch(files))
                .isInstanceOf(PanoramaStitchFailedException.class)
                .hasMessageContaining("겹치는 부분을 찾지 못했습니다");
    }

    @Test
    void stitch_throws_whenPhotoIsNotImage() {
        List<MultipartFile> files = new ArrayList<>(guidedPhotos(scene));
        files.set(3, new MockMultipartFile("files", "broken.jpg", "image/jpeg", new byte[]{1, 2, 3}));

        assertThatThrownBy(() -> panoramaStitchClient.stitch(files))
                .isInstanceOf(PanoramaStitchFailedException.class)
                .hasMessage("사진을 읽을 수 없습니다.");
    }

    /** 가이드 순서(수평 45° 간격 8장 → 천장 → 바닥)대로 찍은 사진. */
    private static List<MultipartFile> guidedPhotos(Mat source) {
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            files.add(jpeg(photo(source, i * 45, 0), "photo" + i + ".jpg"));
        }
        files.add(jpeg(photo(source, 0, 90), "ceiling.jpg"));
        files.add(jpeg(photo(source, 0, -90), "floor.jpg"));
        return files;
    }

    /**
     * 등장방형 장면에서 원근 사진 한 장을 만든다. yaw는 오른쪽으로 돈 각도, pitch는 위로 든 각도.
     * 휴대폰을 가로로 든 채 젖히거나 숙인 방향이라 x축(사진 가로)은 늘 수평이다.
     */
    private static Mat photo(Mat source, double yawDegrees, double pitchDegrees) {
        double focal = PHOTO_WIDTH / 2.0 / Math.tan(Math.toRadians(PHOTO_HORIZONTAL_FOV_DEGREES / 2));
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);

        Mat xMap = new Mat(PHOTO_HEIGHT, PHOTO_WIDTH, CV_32FC1);
        Mat yMap = new Mat(PHOTO_HEIGHT, PHOTO_WIDTH, CV_32FC1);
        try (FloatIndexer xIndexer = xMap.createIndexer(); FloatIndexer yIndexer = yMap.createIndexer()) {
            for (int y = 0; y < PHOTO_HEIGHT; y++) {
                for (int x = 0; x < PHOTO_WIDTH; x++) {
                    // 카메라 좌표(x 오른쪽, y 아래, z 앞) → 위로 pitch만큼 젖힘 → 오른쪽으로 yaw만큼 돌림
                    double cameraX = (x - PHOTO_WIDTH / 2.0) / focal;
                    double cameraY = (y - PHOTO_HEIGHT / 2.0) / focal;
                    double pitchedY = cameraY * Math.cos(pitch) - Math.sin(pitch);
                    double pitchedZ = cameraY * Math.sin(pitch) + Math.cos(pitch);
                    double worldX = cameraX * Math.cos(yaw) + pitchedZ * Math.sin(yaw);
                    double worldZ = -cameraX * Math.sin(yaw) + pitchedZ * Math.cos(yaw);

                    double norm = Math.sqrt(worldX * worldX + pitchedY * pitchedY + worldZ * worldZ);
                    double u = Math.atan2(worldX, worldZ);
                    double v = Math.acos(pitchedY / norm);
                    xIndexer.put(y, x, (float) ((u / (2 * Math.PI) + 0.5) * source.cols()));
                    yIndexer.put(y, x, (float) ((Math.PI - v) / Math.PI * source.rows()));
                }
            }
        }

        Mat photo = new Mat();
        remap(source, photo, xMap, yMap, INTER_LINEAR, BORDER_WRAP, new Scalar(0));
        return photo;
    }

    /** 특징점이 잡히도록 크고 작은 얼룩이 섞인 무늬. */
    private static Mat randomTexture(int width, int height, long seed) {
        theRNG().state(seed);
        Mat coarse = new Mat(height / 16, width / 16, CV_8UC3);
        randu(coarse, new Mat(1, 1, CV_64F, new Scalar(0)), new Mat(1, 1, CV_64F, new Scalar(256)));
        Mat texture = new Mat();
        resize(coarse, texture, new Size(width, height), 0, 0, INTER_LINEAR);

        Mat fine = new Mat(height, width, CV_8UC3);
        randu(fine, new Mat(1, 1, CV_64F, new Scalar(0)), new Mat(1, 1, CV_64F, new Scalar(256)));
        GaussianBlur(fine, fine, new Size(0, 0), 1.5);
        addWeighted(texture, 0.6, fine, 0.4, 0, texture);
        return texture;
    }

    private static MultipartFile jpeg(Mat image, String name) {
        BytePointer buffer = new BytePointer();
        imencode(".jpg", image, buffer);
        byte[] bytes = new byte[(int) buffer.limit()];
        buffer.get(bytes);
        return new MockMultipartFile("files", name, "image/jpeg", bytes);
    }
}
