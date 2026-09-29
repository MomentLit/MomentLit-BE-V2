package com.example.image.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.util.ResponseUtil;
import com.example.image.dto.response.ImageUploadResponse;
import com.example.image.service.ImageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @RequestPart("file") MultipartFile file
    ) {
        ImageUploadResponse response = imageService.upload(file);

        return ResponseEntity.ok(ResponseUtil.success("이미지 업로드에 성공했습니다.", response));
    }

    @PostMapping(value = "/panorama/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadPanorama(
            @RequestPart("file") MultipartFile file
    ) {
        ImageUploadResponse response = imageService.uploadPanorama(file);

        return ResponseEntity.ok(ResponseUtil.success("360도 사진 업로드에 성공했습니다.", response));
    }

    @PostMapping(value = "/panorama/stitch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> stitchPanorama(
            @RequestPart("files") List<MultipartFile> files
    ) {
        ImageUploadResponse response = imageService.stitchPanorama(files);

        return ResponseEntity.ok(ResponseUtil.success("360도 사진 합성에 성공했습니다.", response));
    }
}
