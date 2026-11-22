package com.example.room.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import com.example.room.dto.request.CustomFurnitureFileRequest;
import com.example.room.dto.request.CustomFurniturePhotoRequest;
import com.example.room.dto.request.CustomFurnitureUpdateRequest;
import com.example.room.dto.response.CustomFurnitureResponse;
import com.example.room.service.CustomFurnitureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 내 가구 — 만든 사람만 보고 쓴다(/rooms/** 는 로그인 필수).
 * 사진으로 만들기는 202로 PROCESSING을 돌려주고, 프론트는 목록(또는 단건)을 폴링해 READY/FAILED를 기다린다.
 */
@RestController
@RequiredArgsConstructor
public class CustomFurnitureController {

    private final CustomFurnitureService customFurnitureService;

    @GetMapping("/rooms/furniture")
    public ResponseEntity<ApiResponse<List<CustomFurnitureResponse>>> getMine(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ResponseUtil.success("select my furniture", customFurnitureService.getMine(principal.userId())));
    }

    @GetMapping("/rooms/furniture/{furniture-id}")
    public ResponseEntity<ApiResponse<CustomFurnitureResponse>> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("furniture-id") Long furnitureId
    ) {
        return ResponseEntity.ok(ResponseUtil.success("select my furniture", customFurnitureService.get(principal.userId(), furnitureId)));
    }

    @PostMapping(value = "/rooms/furniture/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CustomFurnitureResponse>> createFromPhoto(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute CustomFurniturePhotoRequest request,
            @RequestPart("front") MultipartFile front,
            @RequestPart(value = "side", required = false) MultipartFile side
    ) {
        CustomFurnitureResponse response = customFurnitureService.createFromPhoto(principal.userId(), request, front, side);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ResponseUtil.success("create my furniture from photo", response));
    }

    @PostMapping(value = "/rooms/furniture/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CustomFurnitureResponse>> createFromFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute CustomFurnitureFileRequest request,
            @RequestPart("model") MultipartFile model,
            @RequestPart("thumbnail") MultipartFile thumbnail
    ) {
        CustomFurnitureResponse response = customFurnitureService.createFromFile(principal.userId(), request, model, thumbnail);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseUtil.success("create my furniture from file", response));
    }

    @PatchMapping("/rooms/furniture/{furniture-id}")
    public ResponseEntity<ApiResponse<CustomFurnitureResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("furniture-id") Long furnitureId,
            @Valid @RequestBody CustomFurnitureUpdateRequest request
    ) {
        return ResponseEntity.ok(ResponseUtil.success("update my furniture", customFurnitureService.update(principal.userId(), furnitureId, request)));
    }

    @DeleteMapping("/rooms/furniture/{furniture-id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("furniture-id") Long furnitureId
    ) {
        customFurnitureService.delete(principal.userId(), furnitureId);
        return ResponseEntity.ok(ResponseUtil.success("delete my furniture"));
    }
}
