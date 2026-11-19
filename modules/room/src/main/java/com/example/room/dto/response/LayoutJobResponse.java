package com.example.room.dto.response;

import com.example.room.service.LayoutJob;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 추론 작업 상태. status가 SUCCEEDED면 result, FAILED면 error가 채워진다. */
public record LayoutJobResponse(
        @JsonProperty("job_id")
        String jobId,

        LayoutJob.Status status,

        Result result,

        String error
) {

    public record Result(
            @JsonProperty("image_url")
            String imageUrl,

            List<List<Double>> uv,

            double z0,

            double z1,

            boolean aligned
    ) {
    }

    public static LayoutJobResponse from(LayoutJob job) {
        LayoutJob.Result r = job.result();
        return new LayoutJobResponse(
                job.id(),
                job.status(),
                r == null ? null : new Result(r.imageUrl(), r.uv(), r.z0(), r.z1(), r.aligned()),
                job.error()
        );
    }
}
