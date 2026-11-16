package com.example.matching.dto.response;

import com.example.matching.entity.Matching;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/** 공간 상세의 시간 선택기가 이미 찬 시간대를 막을 수 있도록 승인된 예약의 시간만 내려준다(예약자 정보 없음). */
public record BookedTimeListResponse(
        @JsonProperty("booked_times")
        List<BookedTime> bookedTimes
) {

    public record BookedTime(
            @JsonProperty("start_time")
            LocalDateTime startTime,

            @JsonProperty("end_time")
            LocalDateTime endTime
    ) {
    }

    public static BookedTimeListResponse from(List<Matching> matchings) {
        return new BookedTimeListResponse(
                matchings.stream()
                        .map(matching -> new BookedTime(matching.getStartTime(), matching.getEndTime()))
                        .toList()
        );
    }
}
