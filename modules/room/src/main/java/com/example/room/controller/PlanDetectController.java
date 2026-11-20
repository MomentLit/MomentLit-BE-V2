package com.example.room.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.util.ResponseUtil;
import com.example.room.dto.request.PlanDetectRequest;
import com.example.room.dto.response.PlanDetectResponse;
import com.example.room.service.PlanDetectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 평면 도면에서 벽 찾기(로그인 필요). 결과는 저장하지 않고 화면의 초안으로만 쓴다. */
@RestController
@RequiredArgsConstructor
public class PlanDetectController {

    private final PlanDetectService planDetectService;

    @PostMapping("/rooms/plan-detect")
    public ResponseEntity<ApiResponse<PlanDetectResponse>> detect(@Valid @RequestBody PlanDetectRequest request) {
        return ResponseEntity.ok(ResponseUtil.success("detect plan walls", planDetectService.detect(request)));
    }
}
