package com.example.popup.dto.response;

import com.example.popup.entity.Popup;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PopupCreateResponse(
        @JsonProperty("popup_id")
        Long popupId
) {

    public static PopupCreateResponse from(Popup popup) {
        return new PopupCreateResponse(popup.getId());
    }
}
