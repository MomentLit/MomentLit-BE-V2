package com.example.image.global.client;

import static org.bytedeco.opencv.global.opencv_core.BORDER_CONSTANT;
import static org.bytedeco.opencv.global.opencv_core.BORDER_REFLECT;
import static org.bytedeco.opencv.global.opencv_core.BORDER_WRAP;
import static org.bytedeco.opencv.global.opencv_core.CV_16S;
import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.CV_32FC1;
import static org.bytedeco.opencv.global.opencv_core.CV_64F;
import static org.bytedeco.opencv.global.opencv_core.CV_8U;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_core.bitwise_and;
import static org.bytedeco.opencv.global.opencv_core.bitwise_not;
import static org.bytedeco.opencv.global.opencv_core.copyMakeBorder;
import static org.bytedeco.opencv.global.opencv_core.countNonZero;
import static org.bytedeco.opencv.global.opencv_core.subtract;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.bytedeco.opencv.global.opencv_imgproc.GaussianBlur;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_AREA;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_LINEAR;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_NEAREST;
import static org.bytedeco.opencv.global.opencv_imgproc.THRESH_BINARY;
import static org.bytedeco.opencv.global.opencv_imgproc.dilate;
import static org.bytedeco.opencv.global.opencv_imgproc.erode;
import static org.bytedeco.opencv.global.opencv_imgproc.remap;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_imgproc.threshold;
import static org.bytedeco.opencv.global.opencv_photo.INPAINT_TELEA;
import static org.bytedeco.opencv.global.opencv_photo.inpaint;
import static org.bytedeco.opencv.global.opencv_stitching.WAVE_CORRECT_HORIZ;
import static org.bytedeco.opencv.global.opencv_stitching.computeImageFeatures;
import static org.bytedeco.opencv.global.opencv_stitching.leaveBiggestComponent;
import static org.bytedeco.opencv.global.opencv_stitching.waveCorrect;

import com.example.image.global.exception.PanoramaStitchFailedException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.PointVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.UMat;
import org.bytedeco.opencv.opencv_core.UMatVector;
import org.bytedeco.opencv.opencv_features2d.SIFT;
import org.bytedeco.opencv.opencv_stitching.BestOf2NearestMatcher;
import org.bytedeco.opencv.opencv_stitching.BlocksGainCompensator;
import org.bytedeco.opencv.opencv_stitching.BundleAdjusterRay;
import org.bytedeco.opencv.opencv_stitching.CameraParams;
import org.bytedeco.opencv.opencv_stitching.CameraParamsVector;
import org.bytedeco.opencv.opencv_stitching.DetailSphericalWarper;
import org.bytedeco.opencv.opencv_stitching.GraphCutSeamFinder;
import org.bytedeco.opencv.opencv_stitching.GraphCutSeamFinderBase;
import org.bytedeco.opencv.opencv_stitching.HomographyBasedEstimator;
import org.bytedeco.opencv.opencv_stitching.ImageFeaturesVector;
import org.bytedeco.opencv.opencv_stitching.MatchesInfoVector;
import org.bytedeco.opencv.opencv_stitching.MultiBandBlender;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 360도 사진 합성 — OpenCV stitching의 detail 파이프라인(특징점 → 매칭 → 카메라 추정 → 구면 워핑 → 블렌딩)을
 * 백엔드 프로세스 안에서 실행한다. 흐름은 OpenCV 예제 stitching_detailed.cpp와 같다.
 * https://github.com/opencv/opencv/blob/4.x/samples/cpp/stitching_detailed.cpp
 *
 * 상위 API(`Stitcher`)는 사진이 덮은 영역만큼만 잘라서 내보내서, 뷰어가 이를 2:1 전체 구면으로 늘려 보여주는 문제가 있었다.
 * 여기서는 구면 워퍼의 scale s를 기준으로 가로 2πs × 세로 πs, 즉 정확한 2:1 등장방형 캔버스에 각 사진을 제자리에
 * 배치한다. 정면(1번) 사진이 캔버스 가운데에 온다. 사진이 덮지 못한 곳은 주변 색으로 채운다.
 *
 * 수평 사진 8장만 특징점으로 이어 붙이고, 천장·바닥은 가이드대로 정면에서 위·아래를 향해 찍었다고 보고 놓는다 —
 * 천장·바닥 사진은 수평 사진과 겹치는 곳이 거의 없고 무늬가 단조로워서, 매칭하면 엉뚱한 곳에 붙어 전체 합성을 망친다.
 */
@Slf4j
@Component
public class PanoramaStitchClient {

    // 프론트 PANORAMA_CAPTURE_STEPS 순서: 수평 8장 → 천장 → 바닥
    private static final int HORIZONTAL_PHOTO_COUNT = 8;

    private static final int CEILING_PHOTO_INDEX = 8;

    private static final int FLOOR_PHOTO_INDEX = 9;

    private static final String OUTPUT_CONTENT_TYPE = "image/jpeg";

    // 특징점 검출·카메라 추정용 해상도 — 프론트가 긴 변 1600px(약 1.9MP)로 줄여 보내므로 사실상 그대로 쓴다.
    // 흰 벽이 많은 실내는 해상도를 낮추면 특징점이 너무 적어진다.
    private static final double REGISTRATION_MEGAPIX = 2.0;

    // 시임 계산용 해상도(약 0.1MP) — stitching_detailed 기본값
    private static final double SEAM_MEGAPIX = 0.1;

    // SIFT 기본값(0.04)보다 낮춰서 흰 벽·유리처럼 대비가 약한 곳에서도 특징점을 찾는다.
    private static final double SIFT_CONTRAST_THRESHOLD = 0.01;

    // 가장 가까운 특징점이 두 번째보다 (1 - 0.3)배 이상 가까워야 매칭으로 본다(Lowe ratio 0.7).
    private static final float MATCH_CONFIDENCE = 0.3f;

    // 이보다 낮은 매칭 신뢰도의 사진 쌍은 이어지지 않은 것으로 본다. 바로 이웃한 사진끼리만 매칭하므로
    // 전체 쌍을 비교하는 Stitcher 기본값(1.0)보다 낮게 둔다.
    private static final float CONNECTION_CONFIDENCE = 0.3f;

    // 신뢰도가 기준을 넘어도 맞는 특징점이 이보다 적으면 우연히 맞은 것으로 본다(호모그래피 최소 4점짜리 우연 매칭을 거른다).
    private static final int MIN_PAIR_INLIERS = 12;

    // 이웃한 수평 사진 사이 각도의 허용 범위 — 가이드는 45°씩 돈다.
    private static final double MIN_HORIZONTAL_STEP_DEGREES = 20;

    private static final double MAX_HORIZONTAL_STEP_DEGREES = 75;

    // 결과 사진 가로 최대 길이 — 블렌딩 메모리와 뷰어 텍스처 크기를 고려한 값
    private static final int MAX_PANORAMA_WIDTH = 6144;

    // 빈 곳 채우기는 이 가로 길이로 줄여서 계산한 뒤 키운다 — 원본 크기로 하면 너무 느리고, 어차피 흐린 색만 필요하다.
    private static final int FILL_WIDTH = 1024;

    // 줄인 이미지 기준 채운 색을 부드럽게 만드는 흐림 정도
    private static final double FILL_BLUR_SIGMA = 6;

    // 사진과 채운 곳의 경계를 부드럽게 섞는 폭(px)
    private static final int FILL_FEATHER = 15;

    public StitchedPanorama stitch(List<MultipartFile> files) {
        List<byte[]> images = files.stream()
                .map(this::readBytes)
                .toList();

        try (PointerScope scope = new PointerScope()) {
            Mat panorama = stitchImages(images);

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

    private Mat stitchImages(List<byte[]> images) {
        List<Mat> sources = new ArrayList<>(images.size());
        for (byte[] image : images) {
            Mat mat = imdecode(new Mat(image), IMREAD_COLOR);
            if (mat.empty()) {
                throw new PanoramaStitchFailedException("사진을 읽을 수 없습니다.");
            }
            sources.add(mat);
        }

        double workScale = Math.min(1.0, Math.sqrt(REGISTRATION_MEGAPIX * 1e6 / sources.get(0).total()));
        double seamScale = Math.min(1.0, Math.sqrt(SEAM_MEGAPIX * 1e6 / sources.get(0).total()));

        CameraParams[] cameras = estimateCameras(sources, workScale);

        // 카메라 값은 workScale 이미지 기준이다. 결과 크기를 정한 뒤 합성용 배율로 바꾼다.
        double medianFocal = medianFocal(cameras);
        double composeScale = Math.min(1.0, MAX_PANORAMA_WIDTH / (2 * Math.PI * medianFocal / workScale));
        double warpScale = medianFocal / workScale * composeScale;

        Mat panoramaMask = new Mat();
        Mat panorama = compose(sources, cameras, workScale, seamScale, composeScale, warpScale, panoramaMask);

        fillUncovered(panorama, panoramaMask);
        return panorama;
    }

    /** 수평 사진의 특징점 매칭으로 카메라(초점거리·회전)를 구하고, 천장·바닥 카메라를 덧붙인다. 결과는 사진 순서와 같은 배열. */
    private CameraParams[] estimateCameras(List<Mat> sources, double workScale) {
        int horizontalCount = Math.min(sources.size(), HORIZONTAL_PHOTO_COUNT);

        MatVector workImages = new MatVector(horizontalCount);
        for (int i = 0; i < horizontalCount; i++) {
            Mat work = new Mat();
            resize(sources.get(i), work, new Size(), workScale, workScale, INTER_LINEAR);
            workImages.put(i, work);
        }

        ImageFeaturesVector features = new ImageFeaturesVector();
        computeImageFeatures(SIFT.create(0, 3, SIFT_CONTRAST_THRESHOLD, 10, 1.6, false), workImages, features);
        for (int i = 0; i < horizontalCount; i++) {
            features.get(i).img_idx(i);
        }

        MatchesInfoVector pairwiseMatches = new MatchesInfoVector();
        BestOf2NearestMatcher matcher = new BestOf2NearestMatcher(false, MATCH_CONFIDENCE, 6, 6, 3.0);
        matcher.apply2(features, pairwiseMatches, toUMat(createNeighborMask(horizontalCount)));
        matcher.collectGarbage();
        for (long i = 0; i < pairwiseMatches.size(); i++) {
            if (pairwiseMatches.get(i).num_inliers() < MIN_PAIR_INLIERS) {
                pairwiseMatches.get(i).confidence(0);
            }
        }

        // 이어지지 않은 사진은 빠지고, 남은 사진의 원래 순번이 돌아온다.
        IntPointer kept = leaveBiggestComponent(features, pairwiseMatches, CONNECTION_CONFIDENCE);
        int[] keptIndices = new int[(int) kept.limit()];
        kept.get(keptIndices);

        List<Integer> keptList = Arrays.stream(keptIndices).boxed().toList();
        List<Integer> missing = IntStream.range(0, horizontalCount)
                .filter(i -> !keptList.contains(i))
                .boxed()
                .toList();
        if (!missing.isEmpty()) {
            log.warn("360도 사진 합성 실패 — 이어지지 않은 수평 사진: {}", missing);
            throw new PanoramaStitchFailedException(
                    formatPhotoNumbers(missing) + " 사진이 이웃한 사진과 겹치는 부분을 찾지 못했습니다. "
                            + "가이드대로 이웃한 사진이 1/3 정도 겹치게 다시 찍어 주세요."
            );
        }

        CameraParamsVector estimated = new CameraParamsVector();
        if (!new HomographyBasedEstimator().apply(features, pairwiseMatches, estimated)) {
            throw new PanoramaStitchFailedException("360도 사진의 카메라 방향을 계산하지 못했습니다. 다시 찍어 주세요.");
        }
        for (int i = 0; i < estimated.size(); i++) {
            Mat rotation = new Mat();
            estimated.get(i).R().convertTo(rotation, CV_32F);
            estimated.get(i).R(rotation);
        }

        BundleAdjusterRay adjuster = new BundleAdjusterRay();
        adjuster.setConfThresh(CONNECTION_CONFIDENCE);
        if (!adjuster.apply(features, pairwiseMatches, estimated)) {
            throw new PanoramaStitchFailedException("360도 사진의 카메라 방향을 계산하지 못했습니다. 다시 찍어 주세요.");
        }

        // 수평선이 휘지 않도록 전체 회전을 바로잡는다.
        MatVector rotations = new MatVector(estimated.size());
        for (int i = 0; i < estimated.size(); i++) {
            rotations.put(i, estimated.get(i).R().clone());
        }
        waveCorrect(rotations, WAVE_CORRECT_HORIZ);

        CameraParams[] cameras = new CameraParams[sources.size()];
        for (int k = 0; k < keptIndices.length; k++) {
            CameraParams camera = new CameraParams(estimated.get(k));
            camera.R(rotations.get(k));
            cameras[keptIndices[k]] = camera;
        }

        // 정면(1번) 사진이 결과 가운데(u = 0)에 오도록 전체를 수평으로 돌린다.
        Mat facingFront = rotationY(-yawOf(cameras[0].R()));
        for (int i = 0; i < horizontalCount; i++) {
            cameras[i].R(multiply(facingFront, cameras[i].R()));
        }
        validateHorizontalSteps(cameras, horizontalCount);

        double focal = medianFocal(cameras);
        placeVerticalPhoto(cameras, CEILING_PHOTO_INDEX, sources, workScale, focal, true);
        placeVerticalPhoto(cameras, FLOOR_PHOTO_INDEX, sources, workScale, focal, false);

        return cameras;
    }

    /** 수평 사진은 바로 앞·뒤 사진과만 매칭한다 — 비슷한 물건(화이트보드·난간 등)끼리 잘못 붙는 것을 막는다. */
    private Mat createNeighborMask(int count) {
        Mat mask = new Mat(count, count, CV_8UC1, new Scalar(0));
        try (UByteIndexer indexer = mask.createIndexer()) {
            for (int i = 0; i < count; i++) {
                indexer.put(i, (i + 1) % count, 255);
                indexer.put((i + 1) % count, i, 255);
            }
        }
        return mask;
    }

    /**
     * 이웃한 수평 사진이 가이드처럼 오른쪽으로 45° 안팎씩 돌아가며 이어졌는지 확인한다 — 무늬가 비슷한 곳끼리 잘못 매칭되면
     * 사진이 엉뚱한 방향에 놓여 결과가 뒤죽박죽이 되므로, 그런 경우는 다시 찍게 한다.
     */
    private void validateHorizontalSteps(CameraParams[] cameras, int horizontalCount) {
        List<Integer> misplaced = new ArrayList<>();
        for (int i = 0; i < horizontalCount; i++) {
            double step = Math.toDegrees(yawOf(cameras[(i + 1) % horizontalCount].R()) - yawOf(cameras[i].R()));
            step = ((step % 360) + 360) % 360;
            if (step < MIN_HORIZONTAL_STEP_DEGREES || step > MAX_HORIZONTAL_STEP_DEGREES) {
                misplaced.add(i);
            }
        }

        if (!misplaced.isEmpty()) {
            log.warn("360도 사진 합성 실패 — 가이드와 다른 각도로 이어진 수평 사진 쌍(앞 사진 기준): {}", misplaced);
            String pairs = misplaced.stream()
                    .map(i -> (i + 1) + "·" + ((i + 1) % horizontalCount + 1) + "번")
                    .collect(Collectors.joining(", "));
            throw new PanoramaStitchFailedException(
                    pairs + " 사진이 가이드 방향(오른쪽으로 45°씩)대로 이어지지 않았습니다. 가이드대로 다시 찍어 주세요."
            );
        }
    }

    /** 가로로 든 휴대폰을 정면에서 그대로 위로 젖힌(천장) / 아래로 숙인(바닥) 방향 — x축 기준 90도 회전. */
    private void placeVerticalPhoto(
            CameraParams[] cameras, int index, List<Mat> sources, double workScale, double focal, boolean ceiling
    ) {
        if (index >= sources.size()) {
            return;
        }

        Mat source = sources.get(index);
        CameraParams camera = new CameraParams();
        camera.focal(focal);
        camera.aspect(1.0);
        camera.ppx(source.cols() * workScale / 2);
        camera.ppy(source.rows() * workScale / 2);
        camera.R(rotationX(ceiling ? Math.PI / 2 : -Math.PI / 2));
        camera.t(new Mat(3, 1, CV_64F, new Scalar(0)));
        cameras[index] = camera;
    }

    /**
     * 구면 워핑 → 노출 보정 → 시임 → 멀티밴드 블렌딩. 2:1 전체 구면 크기의 결과를 돌려주고, 사진이 덮은 곳을 mask에 채운다.
     */
    private Mat compose(
            List<Mat> sources, CameraParams[] cameras, double workScale, double seamScale, double composeScale,
            double warpScale, Mat mask
    ) {
        int count = sources.size();
        double seamWorkAspect = seamScale / workScale;
        double composeWorkAspect = composeScale / workScale;

        // 1) 작은 해상도로 워핑해서 노출 보정값과 시임을 구한다.
        float seamWarpScale = (float) (warpScale * seamScale / composeScale);
        DetailSphericalWarper seamWarper = new DetailSphericalWarper(seamWarpScale);
        PointVector seamCorners = new PointVector(count);
        List<Mat> seamImages = new ArrayList<>(count);
        List<Mat> seamMasks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Mat image = new Mat();
            resize(sources.get(i), image, new Size(), seamScale, seamScale, INTER_LINEAR);
            Mat sourceMask = new Mat(image.size(), CV_8U, new Scalar(255));

            Mat k = scaledIntrinsics(cameras[i], seamWorkAspect);
            Mat warped = new Mat();
            Point corner = warpPhoto(i, seamWarper, seamWarpScale, image, k, cameras[i].R(), INTER_LINEAR, BORDER_REFLECT, warped);
            Mat warpedMask = new Mat();
            warpPhoto(i, seamWarper, seamWarpScale, sourceMask, k, cameras[i].R(), INTER_NEAREST, BORDER_CONSTANT, warpedMask);

            seamCorners.put(i, new Point(corner.x(), corner.y()));
            seamImages.add(warped);
            seamMasks.add(warpedMask);
        }

        BlocksGainCompensator compensator = new BlocksGainCompensator();
        UMatVector compensatorImages = new UMatVector(count);
        UMatVector compensatorMasks = new UMatVector(count);
        for (int i = 0; i < count; i++) {
            compensatorImages.put(i, toUMat(seamImages.get(i)));
            compensatorMasks.put(i, toUMat(seamMasks.get(i)));
        }
        compensator.feed(seamCorners, compensatorImages, compensatorMasks);

        UMatVector seamFloatImages = new UMatVector(count);
        UMatVector seamFoundMasks = new UMatVector(count);
        for (int i = 0; i < count; i++) {
            Mat floatImage = new Mat();
            seamImages.get(i).convertTo(floatImage, CV_32F);
            seamFloatImages.put(i, toUMat(floatImage));
            seamFoundMasks.put(i, toUMat(seamMasks.get(i)));
        }
        new GraphCutSeamFinder(GraphCutSeamFinderBase.COST_COLOR, 10000f, 1000f)
                .find(seamFloatImages, seamCorners, seamFoundMasks);
        for (int i = 0; i < count; i++) {
            seamFoundMasks.get(i).copyTo(seamMasks.get(i));
        }

        // 2) 합성 해상도로 워핑해서 블렌딩한다.
        // 구면 워퍼 좌표: 가로 u ∈ [-πs, πs], 세로 v ∈ [0, πs]. 정면이 u = 0이 되도록 카메라를 맞춰 두었다.
        int height = (int) Math.round(Math.PI * warpScale);
        Rect canvas = new Rect(-height, 0, height * 2, height);

        DetailSphericalWarper warper = new DetailSphericalWarper((float) warpScale);
        MultiBandBlender blender = new MultiBandBlender(0, 5, CV_32F);
        double blendWidth = Math.sqrt((double) canvas.area()) * 0.05;
        blender.setNumBands((int) Math.ceil(Math.log(blendWidth) / Math.log(2)) - 1);
        blender.prepare(canvas);

        Mat dilateKernel = new Mat();
        for (int i = 0; i < count; i++) {
            try (PointerScope imageScope = new PointerScope()) {
                Mat image = new Mat();
                resize(sources.get(i), image, new Size(), composeScale, composeScale, INTER_LINEAR);
                Mat sourceMask = new Mat(image.size(), CV_8U, new Scalar(255));

                Mat k = scaledIntrinsics(cameras[i], composeWorkAspect);
                Mat warped = new Mat();
                Point corner = warpPhoto(i, warper, (float) warpScale, image, k, cameras[i].R(), INTER_LINEAR, BORDER_REFLECT, warped);
                Mat warpedMask = new Mat();
                warpPhoto(i, warper, (float) warpScale, sourceMask, k, cameras[i].R(), INTER_NEAREST, BORDER_CONSTANT, warpedMask);

                compensator.apply(i, corner, warped, warpedMask);

                Mat warpedShort = new Mat();
                warped.convertTo(warpedShort, CV_16S);

                // 작은 해상도에서 구한 시임을 합성 해상도로 키워 사진 영역과 겹친다.
                Mat seamMask = new Mat();
                dilate(seamMasks.get(i), seamMask, dilateKernel);
                Mat resizedSeamMask = new Mat();
                resize(seamMask, resizedSeamMask, warpedMask.size(), 0, 0, INTER_LINEAR);
                bitwise_and(resizedSeamMask, warpedMask, warpedMask);

                // 반올림 때문에 캔버스 밖으로 1~2px 삐져나온 부분은 잘라낸다 — 블렌더는 캔버스 밖 영역을 받지 않는다.
                Rect inCanvas = intersect(new Rect(corner.x(), corner.y(), warped.cols(), warped.rows()), canvas);
                if (inCanvas.width() <= 0 || inCanvas.height() <= 0) {
                    continue;
                }
                Rect local = new Rect(inCanvas.x() - corner.x(), inCanvas.y() - corner.y(), inCanvas.width(), inCanvas.height());
                blender.feed(new Mat(warpedShort, local), new Mat(warpedMask, local), new Point(inCanvas.x(), inCanvas.y()));
            }
        }

        Mat blended = new Mat();
        blender.blend(blended, mask);

        Mat panorama = new Mat();
        blended.convertTo(panorama, CV_8U);
        return panorama;
    }

    private Point warpPhoto(
            int index, DetailSphericalWarper warper, float scale, Mat image, Mat k, Mat rotation,
            int interpolation, int borderMode, Mat dst
    ) {
        if (index == CEILING_PHOTO_INDEX || index == FLOOR_PHOTO_INDEX) {
            return warpVerticalPhoto(image, k, rotation, scale, index == CEILING_PHOTO_INDEX, interpolation, borderMode, dst);
        }
        return warper.warp(image, k, rotation, interpolation, borderMode, dst);
    }

    /**
     * 천장·바닥 사진을 구면에 펼친다. OpenCV 구면 워퍼는 사진 안에 극점이 들어 있는지 판단할 때 카메라가 정확히 위·아래를
     * 보면 극점을 빠뜨려서(사진 가운데가 비어 보임), 극점을 포함한 가로 전체 띠의 좌표를 직접 계산한다.
     * 계산식은 OpenCV SphericalProjector::mapBackward와 같다.
     */
    private Point warpVerticalPhoto(
            Mat image, Mat k, Mat rotation, float scale, boolean ceiling, int interpolation, int borderMode, Mat dst
    ) {
        float fx, fy, cx, cy;
        try (FloatIndexer indexer = k.createIndexer()) {
            fx = indexer.get(0, 0);
            fy = indexer.get(1, 1);
            cx = indexer.get(0, 2);
            cy = indexer.get(1, 2);
        }
        float[] r = new float[9];
        try (FloatIndexer indexer = rotation.createIndexer()) {
            for (int i = 0; i < 9; i++) {
                r[i] = indexer.get(i / 3, i % 3);
            }
        }

        // 사진 가운데에서 모서리까지의 각도만큼 극점에서 내려온 띠를 덮는다.
        double halfDiagonal = Math.hypot(Math.max(cx, image.cols() - cx), Math.max(cy, image.rows() - cy));
        int fullHeight = (int) Math.round(Math.PI * scale);
        int bandHeight = Math.min(fullHeight, (int) Math.ceil(Math.atan(halfDiagonal / Math.min(fx, fy)) * scale) + 2);
        int width = fullHeight * 2;
        Point corner = new Point(-fullHeight, ceiling ? 0 : fullHeight - bandHeight);

        Mat xMap = new Mat(bandHeight, width, CV_32FC1);
        Mat yMap = new Mat(bandHeight, width, CV_32FC1);
        try (FloatIndexer xIndexer = xMap.createIndexer(); FloatIndexer yIndexer = yMap.createIndexer()) {
            for (int y = 0; y < bandHeight; y++) {
                double v = (y + corner.y()) / scale;
                double sinV = Math.sin(Math.PI - v);
                double rayY = Math.cos(Math.PI - v);
                for (int x = 0; x < width; x++) {
                    double u = (x + corner.x()) / scale;
                    double rayX = sinV * Math.sin(u);
                    double rayZ = sinV * Math.cos(u);

                    // 구면 방향(월드) → 카메라 좌표: Rᵀ · ray
                    double camX = r[0] * rayX + r[3] * rayY + r[6] * rayZ;
                    double camY = r[1] * rayX + r[4] * rayY + r[7] * rayZ;
                    double camZ = r[2] * rayX + r[5] * rayY + r[8] * rayZ;

                    if (camZ > 0) {
                        xIndexer.put(y, x, (float) (fx * camX / camZ + cx));
                        yIndexer.put(y, x, (float) (fy * camY / camZ + cy));
                    } else {
                        xIndexer.put(y, x, -1);
                        yIndexer.put(y, x, -1);
                    }
                }
            }
        }

        remap(image, dst, xMap, yMap, interpolation, borderMode, new Scalar(0));
        return corner;
    }

    /**
     * 사진이 덮지 못한 곳(수평 사진과 천장·바닥 사진 사이)을 주변 색으로 채운다. 줄인 이미지에서 inpaint로 채우고 흐리게
     * 만든 뒤 원래 크기로 키워 빈 곳에만 넣는다. 등장방형의 왼쪽·오른쪽 끝은 이어져 있으므로 좌우를 이어 붙인 채로 계산한다.
     */
    private void fillUncovered(Mat panorama, Mat mask) {
        Mat holes = new Mat();
        threshold(mask, holes, 0, 255, THRESH_BINARY);
        bitwise_not(holes, holes);
        if (countNonZero(holes) == 0) {
            return;
        }

        double fillScale = Math.min(1.0, (double) FILL_WIDTH / panorama.cols());
        Mat small = new Mat();
        resize(panorama, small, new Size(), fillScale, fillScale, INTER_AREA);
        Mat smallHoles = new Mat();
        resize(holes, smallHoles, small.size(), 0, 0, INTER_AREA);
        // 경계에서 검은색이 섞인 픽셀도 빈 곳으로 본다.
        threshold(smallHoles, smallHoles, 0, 255, THRESH_BINARY);

        int pad = small.cols() / 4;
        Mat paddedSmall = new Mat();
        copyMakeBorder(small, paddedSmall, 0, 0, pad, pad, BORDER_WRAP);
        Mat paddedHoles = new Mat();
        copyMakeBorder(smallHoles, paddedHoles, 0, 0, pad, pad, BORDER_WRAP);

        Mat filled = new Mat();
        inpaint(paddedSmall, paddedHoles, filled, 3, INPAINT_TELEA);
        GaussianBlur(filled, filled, new Size(0, 0), FILL_BLUR_SIGMA);

        Mat filledSmall = new Mat(filled, new Rect(pad, 0, small.cols(), small.rows()));
        Mat filledLarge = new Mat();
        resize(filledSmall, filledLarge, panorama.size(), 0, 0, INTER_LINEAR);
        filledLarge.copyTo(panorama, holes);

        // 경계를 따라 띠 모양으로 흐리게 섞어서 사진과 채운 곳이 칼로 자른 듯 나뉘지 않게 한다.
        Mat kernel = new Mat(FILL_FEATHER, FILL_FEATHER, CV_8U, new Scalar(1));
        Mat outer = new Mat();
        dilate(holes, outer, kernel);
        Mat inner = new Mat();
        erode(holes, inner, kernel);
        Mat band = new Mat();
        subtract(outer, inner, band);

        Mat blurred = new Mat();
        GaussianBlur(panorama, blurred, new Size(0, 0), FILL_FEATHER / 2.0);
        blurred.copyTo(panorama, band);
    }

    /** 수평 사진 카메라들의 초점거리 중앙값 — 천장·바닥 카메라와 결과 크기의 기준. */
    private double medianFocal(CameraParams[] cameras) {
        double[] focals = IntStream.range(0, Math.min(cameras.length, HORIZONTAL_PHOTO_COUNT))
                .mapToDouble(i -> cameras[i].focal())
                .sorted()
                .toArray();
        return focals[focals.length / 2];
    }

    /** stitching의 노출 보정·시임 계산은 UMat을 받는다 — 원본 Mat과 메모리를 나누지 않도록 복사한다. */
    private UMat toUMat(Mat mat) {
        UMat umat = new UMat();
        mat.copyTo(umat);
        return umat;
    }

    private Mat scaledIntrinsics(CameraParams camera, double aspect) {
        CameraParams scaled = new CameraParams(camera);
        scaled.focal(camera.focal() * aspect);
        scaled.ppx(camera.ppx() * aspect);
        scaled.ppy(camera.ppy() * aspect);

        Mat k = new Mat();
        scaled.K().convertTo(k, CV_32F);
        return k;
    }

    private Rect intersect(Rect a, Rect b) {
        int x = Math.max(a.x(), b.x());
        int y = Math.max(a.y(), b.y());
        int right = Math.min(a.x() + a.width(), b.x() + b.width());
        int bottom = Math.min(a.y() + a.height(), b.y() + b.height());
        return new Rect(x, y, right - x, bottom - y);
    }

    /** 카메라가 바라보는 방향(회전 행렬의 세 번째 열)의 수평 각도(라디안). */
    private double yawOf(Mat rotation) {
        try (FloatIndexer indexer = rotation.createIndexer()) {
            return Math.atan2(indexer.get(0, 2), indexer.get(2, 2));
        }
    }

    private Mat rotationY(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return matrix(
                cos, 0, sin,
                0, 1, 0,
                -sin, 0, cos
        );
    }

    private Mat rotationX(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return matrix(
                1, 0, 0,
                0, cos, -sin,
                0, sin, cos
        );
    }

    private Mat matrix(double... values) {
        Mat mat = new Mat(3, 3, CV_32F);
        try (FloatIndexer indexer = mat.createIndexer()) {
            for (int i = 0; i < 9; i++) {
                indexer.put(i / 3, i % 3, (float) values[i]);
            }
        }
        return mat;
    }

    private Mat multiply(Mat left, Mat right) {
        Mat result = new Mat(3, 3, CV_32F);
        try (FloatIndexer l = left.createIndexer();
             FloatIndexer r = right.createIndexer();
             FloatIndexer out = result.createIndexer()) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    float sum = 0;
                    for (int k = 0; k < 3; k++) {
                        sum += l.get(i, k) * r.get(k, j);
                    }
                    out.put(i, j, sum);
                }
            }
        }
        return result;
    }

    private String formatPhotoNumbers(List<Integer> indices) {
        return indices.stream()
                .map(i -> String.valueOf(i + 1))
                .collect(Collectors.joining("·")) + "번";
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
