package com.example.space.entity;

/**
 * 공간의 예약 방식.
 * INTERNAL은 모먼트릿 예약(매칭) 요청을 받고, EXTERNAL은 호스트의 외부 홈페이지로 예약을 넘긴다.
 */
public enum BookingType {
    INTERNAL,
    EXTERNAL
}
