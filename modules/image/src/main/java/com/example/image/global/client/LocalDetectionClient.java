package com.example.image.global.client;

import static org.bytedeco.opencv.global.opencv_core.BORDER_CONSTANT;
import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.copyMakeBorder;
import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_AREA;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_LINEAR;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.example.image.global.exception.PrivacyBlurFailedException;
import jakarta.annotation.PreDestroy;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.FaceDetectorYN;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 사진 속 사람 얼굴·차량 번호판 위치 검출 — 서버 안에서 로컬 AI 모델로 실행한다(외부 API·사용량 제한 없음).
 * 흐림 처리는 하지 않는다(PrivacyBlurService가 OpenCV로 처리).
 *  - 얼굴: YuNet (OpenCV FaceDetectorYN) https://github.com/opencv/opencv_zoo/tree/main/models/face_detection_yunet
 *  - 번호판: open-image-models YOLOv9 end2end (ONNX Runtime) https://github.com/ankandrew/open-image-models
 * 결과 좌표는 box = [ymin, xmin, ymax, xmax], 0~1000으로 정규화된 값.
 */
@Slf4j
@Component
public class LocalDetectionClient {

    private static final String FACE_MODEL = "models/face_detection_yunet_2023mar.onnx";

    private static final String PLATE_MODEL = "models/yolo-v9-t-640-license-plates-end2end.onnx";

    private static final int NORMALIZED_MAX = 1000;

    // 놓치지 않는 쪽으로 낮게 잡는다 — 얼굴이 아닌 곳이 가끔 흐려지는 편이 얼굴을 놓치는 것보다 낫다.
    private static final float FACE_SCORE_THRESHOLD = 0.6f;

    private static final float FACE_NMS_THRESHOLD = 0.3f;

    private static final int FACE_TOP_K = 5000;

    // YuNet은 대략 10~300px 얼굴을 잘 찾는다 — 큰 얼굴을 놓치지 않도록 이 크기로 줄인 이미지에서도 한 번 더 찾는다.
    private static final int FACE_LARGE_PASS_SIDE = 640;

    // 모델 제공처(open-image-models) 기본값
    private static final float PLATE_SCORE_THRESHOLD = 0.25f;

    private static final int PLATE_INPUT_SIZE = 640;

    private static final double PLATE_PADDING_COLOR = 114;

    private final String faceModelPath;
    private final OrtEnvironment ortEnvironment;
    private final OrtSession plateSession;

    /** 모델을 불러오지 못해도 앱은 시작한다 — 이후 검출 요청은 실패로 처리되어 원본이 업로드된다. */
    public LocalDetectionClient() {
        this.faceModelPath = loadFaceModel();
        this.ortEnvironment = this.faceModelPath == null ? null : loadOrtEnvironment();
        this.plateSession = this.ortEnvironment == null ? null : loadPlateSession(this.ortEnvironment);
    }

    public List<DetectedRegion> detect(byte[] jpegBytes) {
        if (faceModelPath == null || plateSession == null) {
            throw new PrivacyBlurFailedException("로컬 검출 모델을 불러오지 못했습니다.");
        }

        try (PointerScope scope = new PointerScope()) {
            Mat image = imdecode(new Mat(jpegBytes), IMREAD_COLOR);
            if (image.empty()) {
                throw new PrivacyBlurFailedException("검출할 이미지를 읽을 수 없습니다.");
            }

            List<DetectedRegion> regions = new ArrayList<>(detectFaces(image));
            regions.addAll(detectPlates(image));
            return regions;
        } catch (PrivacyBlurFailedException e) {
            throw e;
        } catch (Exception | LinkageError e) {
            throw new PrivacyBlurFailedException("로컬 검출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /** 원본 크기(작은 얼굴) + 축소본(큰 얼굴) 두 번 찾는다. 같은 얼굴이 겹쳐 나와도 둘 다 흐리면 되므로 합치지 않는다. */
    private List<DetectedRegion> detectFaces(Mat image) {
        List<DetectedRegion> regions = new ArrayList<>(detectFacesAt(image));

        int longSide = Math.max(image.cols(), image.rows());
        if (longSide > FACE_LARGE_PASS_SIDE) {
            double scale = (double) FACE_LARGE_PASS_SIDE / longSide;
            Mat small = new Mat();
            resize(image, small, new Size(
                    Math.max(1, (int) Math.round(image.cols() * scale)),
                    Math.max(1, (int) Math.round(image.rows() * scale))
            ), 0, 0, INTER_AREA);
            regions.addAll(detectFacesAt(small));
        }

        return regions;
    }

    /** FaceDetectorYN은 스레드 간에 공유하면 안 되어 호출마다 새로 만든다(360도 조각은 병렬로 들어온다). */
    private List<DetectedRegion> detectFacesAt(Mat image) {
        int width = image.cols();
        int height = image.rows();

        FaceDetectorYN detector = FaceDetectorYN.create(
                faceModelPath, "", new Size(width, height),
                FACE_SCORE_THRESHOLD, FACE_NMS_THRESHOLD, FACE_TOP_K, 0, 0
        );
        Mat faces = new Mat();
        detector.detect(image, faces);

        List<DetectedRegion> regions = new ArrayList<>();
        if (faces.empty()) {
            return regions;
        }

        // 행마다 [x, y, w, h, 눈·코·입 좌표 10개, score]
        FloatIndexer indexer = faces.createIndexer();
        for (int i = 0; i < faces.rows(); i++) {
            float x = indexer.get(i, 0);
            float y = indexer.get(i, 1);
            float w = indexer.get(i, 2);
            float h = indexer.get(i, 3);
            regions.add(toNormalized("face", x, y, x + w, y + h, width, height));
        }
        indexer.release();
        return regions;
    }

    /** 모델 제공처 전처리와 동일: 비율 유지 리사이즈 + 회색(114) 여백으로 640x640, RGB, 0~1. */
    private List<DetectedRegion> detectPlates(Mat image) throws Exception {
        int width = image.cols();
        int height = image.rows();

        double ratio = Math.min((double) PLATE_INPUT_SIZE / height, (double) PLATE_INPUT_SIZE / width);
        int resizedWidth = (int) Math.round(width * ratio);
        int resizedHeight = (int) Math.round(height * ratio);
        double padX = (PLATE_INPUT_SIZE - resizedWidth) / 2.0;
        double padY = (PLATE_INPUT_SIZE - resizedHeight) / 2.0;

        Mat resized = new Mat();
        resize(image, resized, new Size(resizedWidth, resizedHeight), 0, 0, INTER_LINEAR);
        Mat padded = new Mat();
        copyMakeBorder(
                resized, padded,
                (int) Math.round(padY - 0.1), (int) Math.round(padY + 0.1),
                (int) Math.round(padX - 0.1), (int) Math.round(padX + 0.1),
                BORDER_CONSTANT, new Scalar(PLATE_PADDING_COLOR, PLATE_PADDING_COLOR, PLATE_PADDING_COLOR, 0)
        );

        Mat blob = blobFromImage(
                padded, 1 / 255.0, new Size(PLATE_INPUT_SIZE, PLATE_INPUT_SIZE),
                new Scalar(0, 0, 0, 0), true, false, CV_32F
        );
        float[] input = new float[(int) blob.total()];
        ((FloatBuffer) blob.createBuffer()).get(input);

        String inputName = plateSession.getInputNames().iterator().next();
        try (OnnxTensor tensor = OnnxTensor.createTensor(
                ortEnvironment, FloatBuffer.wrap(input), new long[]{1, 3, PLATE_INPUT_SIZE, PLATE_INPUT_SIZE});
             OrtSession.Result result = plateSession.run(Map.of(inputName, tensor))) {

            // 행마다 [batch, x1, y1, x2, y2, class, score] — 640x640 입력 기준 좌표
            float[][] predictions = (float[][]) result.get(0).getValue();

            List<DetectedRegion> regions = new ArrayList<>();
            for (float[] row : predictions) {
                if (row[6] < PLATE_SCORE_THRESHOLD) {
                    continue;
                }
                regions.add(toNormalized(
                        "license_plate",
                        (row[1] - padX) / ratio, (row[2] - padY) / ratio,
                        (row[3] - padX) / ratio, (row[4] - padY) / ratio,
                        width, height
                ));
            }
            return regions;
        }
    }

    private DetectedRegion toNormalized(String label, double x1, double y1, double x2, double y2, int width, int height) {
        return new DetectedRegion(
                label,
                normalize(y1, height),
                normalize(x1, width),
                normalize(y2, height),
                normalize(x2, width)
        );
    }

    private int normalize(double value, int size) {
        int normalized = (int) Math.round(value * NORMALIZED_MAX / size);
        return Math.max(0, Math.min(NORMALIZED_MAX, normalized));
    }

    /** FaceDetectorYN은 파일 경로만 받아서 jar 안의 모델을 임시 파일로 꺼낸다. 불러오기 확인까지 여기서 한다. */
    private String loadFaceModel() {
        try (InputStream input = new ClassPathResource(FACE_MODEL).getInputStream();
             PointerScope scope = new PointerScope()) {
            Path file = Files.createTempFile("face_detection_yunet", ".onnx");
            file.toFile().deleteOnExit();
            Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);

            FaceDetectorYN.create(file.toString(), "", new Size(FACE_LARGE_PASS_SIDE, FACE_LARGE_PASS_SIDE));
            log.info("얼굴 검출 모델을 불러왔습니다: {}", FACE_MODEL);
            return file.toString();
        } catch (Exception | LinkageError e) {
            log.error("얼굴 검출 모델을 불러오지 못했습니다. 사진 블라인드 없이 원본이 업로드됩니다.", e);
            return null;
        }
    }

    private OrtEnvironment loadOrtEnvironment() {
        try {
            return OrtEnvironment.getEnvironment();
        } catch (Exception | LinkageError e) {
            log.error("ONNX Runtime을 불러오지 못했습니다. 사진 블라인드 없이 원본이 업로드됩니다.", e);
            return null;
        }
    }

    private OrtSession loadPlateSession(OrtEnvironment environment) {
        try (InputStream input = new ClassPathResource(PLATE_MODEL).getInputStream()) {
            OrtSession session = environment.createSession(input.readAllBytes(), new OrtSession.SessionOptions());
            log.info("번호판 검출 모델을 불러왔습니다: {}", PLATE_MODEL);
            return session;
        } catch (Exception | LinkageError e) {
            log.error("번호판 검출 모델을 불러오지 못했습니다. 사진 블라인드 없이 원본이 업로드됩니다.", e);
            return null;
        }
    }

    @PreDestroy
    void close() throws Exception {
        if (plateSession != null) {
            plateSession.close();
        }
    }

    /** 0~1000으로 정규화된 영역. */
    public record DetectedRegion(
            String label,
            int yMin,
            int xMin,
            int yMax,
            int xMax
    ) {
    }
}
