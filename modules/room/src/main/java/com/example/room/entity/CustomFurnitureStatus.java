package com.example.room.entity;

/** 사진 생성은 PROCESSING → READY/FAILED. 파일은 올리자마자 READY. */
public enum CustomFurnitureStatus {
    PROCESSING,
    READY,
    FAILED
}
