package com.example.image.global.exception;

import com.example.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class ImageExceptionHandler {

    @ExceptionHandler(ImageFileMissingException.class)
    public ResponseEntity<ApiResponse<String>> imageFileMissingHandleException(ImageFileMissingException e) {
        log.warn("ImageFileMissingException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("[ERROR: Image/File/Missing] " + e.getMessage()));
    }

    @ExceptionHandler(EmptyImageFileException.class)
    public ResponseEntity<ApiResponse<String>> emptyImageFileHandleException(EmptyImageFileException e) {
        log.warn("EmptyImageFileException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("[ERROR: Image/File/Empty] " + e.getMessage()));
    }

    @ExceptionHandler(InvalidImageTypeException.class)
    public ResponseEntity<ApiResponse<String>> invalidImageTypeHandleException(InvalidImageTypeException e) {
        log.warn("InvalidImageTypeException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("[ERROR: Image/File/InvalidType] " + e.getMessage()));
    }

    @ExceptionHandler(ImageUploadFailedException.class)
    public ResponseEntity<ApiResponse<String>> imageUploadFailedHandleException(ImageUploadFailedException e) {
        log.error("ImageUploadFailedException", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("[ERROR: Image/Upload/Failed] " + e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<String>> maxUploadSizeHandleException(MaxUploadSizeExceededException e) {
        log.warn("MaxUploadSizeExceededException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("[ERROR: Image/File/SizeExceeded] 이미지 파일은 최대 20MB까지 업로드할 수 있습니다."));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<String>> missingRequestPartHandleException(MissingServletRequestPartException e) {
        log.warn("MissingServletRequestPartException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("[ERROR: Image/File/Missing] 이미지 파일이 필요합니다."));
    }

    @ExceptionHandler(ImageException.class)
    public ResponseEntity<ApiResponse<String>> imageHandleException(ImageException e) {
        log.error("ImageException", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("[ERROR: Image/?] " + e.getMessage()));
    }
}
