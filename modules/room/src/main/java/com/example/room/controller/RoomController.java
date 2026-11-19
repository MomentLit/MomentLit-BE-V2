package com.example.room.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import com.example.room.dto.request.FurnitureLayoutRequest;
import com.example.room.dto.request.RoomUpsertRequest;
import com.example.room.dto.response.FurnitureLayoutResponse;
import com.example.room.dto.response.RoomResponse;
import com.example.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /** 공간의 3D 방 — 비로그인도 볼 수 있다. 없으면 404. */
    @GetMapping("/spaces/{space-id}/room")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(@PathVariable("space-id") Long spaceId) {
        return ResponseEntity.ok(ResponseUtil.success("select space room", roomService.getRoom(spaceId)));
    }

    @PutMapping("/spaces/{space-id}/room")
    public ResponseEntity<ApiResponse<RoomResponse>> saveRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId,
            @Valid @RequestBody RoomUpsertRequest request
    ) {
        RoomResponse response = roomService.saveRoom(principal.userId(), principal.role().name(), spaceId, request);
        return ResponseEntity.ok(ResponseUtil.success("save space room", response));
    }

    @DeleteMapping("/spaces/{space-id}/room")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId
    ) {
        roomService.deleteRoom(principal.userId(), principal.role().name(), spaceId);
        return ResponseEntity.ok(ResponseUtil.success("delete space room"));
    }

    /** 내가 저장한 가구 배치. 저장한 적 없으면 data가 null. */
    @GetMapping("/spaces/{space-id}/room/my-layout")
    public ResponseEntity<ApiResponse<FurnitureLayoutResponse>> getMyLayout(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId
    ) {
        FurnitureLayoutResponse response = roomService.getMyLayout(principal.userId(), spaceId).orElse(null);
        return ResponseEntity.ok(ResponseUtil.success("select my room layout", response));
    }

    @PutMapping("/spaces/{space-id}/room/my-layout")
    public ResponseEntity<ApiResponse<FurnitureLayoutResponse>> saveMyLayout(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId,
            @Valid @RequestBody FurnitureLayoutRequest request
    ) {
        FurnitureLayoutResponse response = roomService.saveMyLayout(principal.userId(), spaceId, request);
        return ResponseEntity.ok(ResponseUtil.success("save my room layout", response));
    }

    @DeleteMapping("/spaces/{space-id}/room/my-layout")
    public ResponseEntity<ApiResponse<Void>> deleteMyLayout(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId
    ) {
        roomService.deleteMyLayout(principal.userId(), spaceId);
        return ResponseEntity.ok(ResponseUtil.success("delete my room layout"));
    }
}
