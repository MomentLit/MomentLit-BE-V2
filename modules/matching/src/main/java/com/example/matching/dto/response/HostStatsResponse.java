package com.example.matching.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 호스트별 응답률 / 평균 응답시간 집계 결과.
 * 응답률 = (APPROVED + REJECTED로 처리된 매칭 수) / (REQUESTED 상태를 거친 전체 매칭 수) x 100
 * 평균 응답시간 = 처리된(APPROVED/REJECTED) 매칭들의 (updatedAt - createdAt) 평균(분)
 */
public record HostStatsResponse(
        @JsonProperty("host_id")
        String hostId,

        @JsonProperty("total_requested_count")
        long totalRequestedCount,

        @JsonProperty("processed_count")
        long processedCount,

        @JsonProperty("response_rate")
        Double responseRate,

        @JsonProperty("avg_response_minutes")
        Double avgResponseMinutes
) {

    public static HostStatsResponse of(
            String hostId,
            long totalRequestedCount,
            long processedCount,
            Double avgResponseMinutes
    ) {
        Double responseRate = totalRequestedCount == 0
                ? 0.0
                : Math.round(processedCount * 1000.0 / totalRequestedCount) / 10.0;

        return new HostStatsResponse(
                hostId,
                totalRequestedCount,
                processedCount,
                responseRate,
                avgResponseMinutes
        );
    }
}
