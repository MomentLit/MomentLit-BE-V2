package com.example.room.service;

import com.example.image.service.ImageService;
import com.example.room.dto.request.PlanDetectRequest;
import com.example.room.dto.response.PlanDetectResponse;
import com.example.room.global.client.LayoutClient;
import com.example.room.global.exception.InvalidRoomException;
import com.example.room.global.exception.LayoutRejectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 평면 도면 → 벽 자동 인식. 영상처리만 써서 1~2초면 끝나므로 작업 큐 없이 요청 안에서 바로 돌려준다.
 */
@Service
@RequiredArgsConstructor
public class PlanDetectService {

    private final LayoutClient layoutClient;
    private final ImageService imageService;

    public PlanDetectResponse detect(PlanDetectRequest request) {
        if (!imageService.isOwnImageUrl(request.imageUrl())) {
            throw new InvalidRoomException("이 서비스에 올린 도면 이미지만 쓸 수 있습니다.");
        }
        byte[] image = imageService.downloadOwnImage(request.imageUrl());
        try {
            return layoutClient.detectPlan(image, request.metersPerPixel());
        } catch (LayoutRejectedException e) {
            // 이미지를 읽을 수 없는 경우 등 — 화면에서는 "못 찾음"과 같게 보여 준다
            return PlanDetectResponse.failed(e.getMessage());
        }
    }
}
