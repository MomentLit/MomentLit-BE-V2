package com.example.image.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ImageUploadResponse(
        @JsonProperty("image_url")
        String imageUrl
) {
}
