package com.example.room.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import com.example.room.dto.request.LayoutJobCreateRequest;
import com.example.room.dto.response.LayoutJobResponse;
import com.example.room.service.LayoutJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 360도 사진에서 방 모서리 찾기. 공간 등록 중(공간 id가 아직 없을 때)에도 쓰므로 공간에 묶지 않는다.
 * POST로 작업을 만들고(202), GET으로 status가 SUCCEEDED/FAILED가 될 때까지 폴링한다.
 */
@RestController
@RequiredArgsConstructor
public class LayoutJobController {

    private final LayoutJobService layoutJobService;

    @PostMapping("/rooms/layout-jobs")
    public ResponseEntity<ApiResponse<LayoutJobResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LayoutJobCreateRequest request
    ) {
        LayoutJobResponse response = layoutJobService.create(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ResponseUtil.success("create room layout job", response));
    }

    @GetMapping("/rooms/layout-jobs/{job-id}")
    public ResponseEntity<ApiResponse<LayoutJobResponse>> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("job-id") String jobId
    ) {
        return ResponseEntity.ok(ResponseUtil.success("select room layout job", layoutJobService.get(principal.userId(), jobId)));
    }
}
