package com.example.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC3;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.image.global.client.LocalDetectionClient;
import com.example.image.global.client.LocalDetectionClient.DetectedRegion;
import com.example.image.global.exception.PrivacyBlurFailedException;
import java.util.List;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrivacyBlurServiceTest {

    // 1px 체커보드(0/255)는 흐려지면 중간 밝기가 되고, 흐려지지 않으면 0 또는 255 그대로다.
    private static final int BLACK = 0;
    private static final int WHITE = 255;

    @Mock
    private LocalDetectionClient localDetectionClient;

    private PrivacyBlurService privacyBlurService;

    @BeforeEach
    void setUp() {
        privacyBlurService = new PrivacyBlurService(localDetectionClient);
    }

    @Test
    void blur_returnsOriginal_whenNothingDetected() {
        byte[] original = checkerboardPng(200, 100);
        when(localDetectionClient.detect(any())).thenReturn(List.of());

        byte[] result = privacyBlurService.blur(original, ".png", false);

        assertThat(result).isSameAs(original);
    }

    @Test
    void blur_returnsOriginal_whenDetectionFails() {
        byte[] original = checkerboardPng(200, 100);
        when(localDetectionClient.detect(any())).thenThrow(new PrivacyBlurFailedException("모델 없음"));

        byte[] result = privacyBlurService.blur(original, ".png", false);

        assertThat(result).isSameAs(original);
    }

    @Test
    void blur_blursOnlyDetectedRegionWithPadding() {
        // 200x100 이미지에서 x 50~100, y 20~60 → 10% 여백 → x 45~105, y 16~64
        byte[] original = checkerboardPng(200, 100);
        when(localDetectionClient.detect(any()))
                .thenReturn(List.of(new DetectedRegion("face", 200, 250, 600, 500)));

        Mat result = decode(privacyBlurService.blur(original, ".png", false));

        assertThat(isBlurred(result, 75, 40)).isTrue();
        assertThat(isBlurred(result, 46, 17)).isTrue();
        assertThat(isBlurred(result, 104, 63)).isTrue();
        assertThat(isBlurred(result, 10, 10)).isFalse();
        assertThat(isBlurred(result, 110, 40)).isFalse();
        assertThat(isBlurred(result, 75, 70)).isFalse();
    }

    @Test
    void blur_splitsRegionAcrossPanoramaSeam() {
        // 400px 폭 파노라마: 조각 폭 100, 이음새 조각 = 원본 [350,400) + [0,50) (폭 100, 다른 조각은 110/120)
        byte[] original = checkerboardPng(400, 200);
        when(localDetectionClient.detect(any())).thenAnswer(invocation -> {
            Mat tile = decode(invocation.getArgument(0));
            if (tile.cols() == 100) {
                // 이음새 조각의 x 40~60 → 여백 → 38~62 → 원본의 오른쪽 끝 388~400 + 왼쪽 끝 0~12
                return List.of(new DetectedRegion("face", 300, 400, 700, 600));
            }
            return List.of();
        });

        Mat result = decode(privacyBlurService.blur(original, ".png", true));

        assertThat(isBlurred(result, 395, 100)).isTrue();
        assertThat(isBlurred(result, 5, 100)).isTrue();
        assertThat(isBlurred(result, 200, 100)).isFalse();
        assertThat(isBlurred(result, 380, 100)).isFalse();
        assertThat(isBlurred(result, 20, 100)).isFalse();
    }

    private byte[] checkerboardPng(int width, int height) {
        Mat image = new Mat(height, width, CV_8UC3);
        UByteIndexer indexer = image.createIndexer();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = (x + y) % 2 == 0 ? BLACK : WHITE;
                for (int c = 0; c < 3; c++) {
                    indexer.put(y, x, c, value);
                }
            }
        }
        indexer.release();

        BytePointer buffer = new BytePointer();
        imencode(".png", image, buffer);
        byte[] bytes = new byte[(int) buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }

    private Mat decode(byte[] bytes) {
        return imdecode(new Mat(bytes), IMREAD_COLOR);
    }

    private boolean isBlurred(Mat image, int x, int y) {
        UByteIndexer indexer = image.createIndexer();
        int value = indexer.get(y, x, 0);
        indexer.release();
        return value != BLACK && value != WHITE;
    }
}
