package com.example.alarm.controller;

import com.example.alarm.dto.request.AlarmCreateRequest;
import com.example.alarm.dto.response.AlarmResponse;
import com.example.alarm.service.AlarmService;
import com.example.common.dto.ApiResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/alarm")
public class AlarmController {

    private final AlarmService alarmService;

    @PostMapping("/{matching-id}")
    public ResponseEntity<ApiResponse<Void>> create(
            @PathVariable("matching-id") Long matchingId,
            @Valid @RequestBody AlarmCreateRequest request
    ) {
        alarmService.create(matchingId, request);
        ApiResponse<Void> apiResponse = ResponseUtil.success("create alarm", null);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlarmResponse>>> getAlarms(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<AlarmResponse> alarms = alarmService.getAlarms(principal.userId());
        ApiResponse<List<AlarmResponse>> apiResponse = ResponseUtil.success("get alarms", alarms);

        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/{alarm-id}")
    public ResponseEntity<ApiResponse<Void>> updateRead(
            @PathVariable("alarm-id") Long alarmId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        alarmService.updateRead(principal.userId(), alarmId);
        ApiResponse<Void> apiResponse = ResponseUtil.success("update alarm read", null);

        return ResponseEntity.ok(apiResponse);
    }
}
