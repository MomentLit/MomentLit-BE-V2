package com.example.matching.service;

import com.example.space.entity.UsageUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingPriceCalculatorTest {

    @Test
    void 초과_요금이_없으면_기존과_같이_시간당_요금_곱하기_시간이다() {
        assertThat(BookingPriceCalculator.calculate(30000, false, UsageUnit.HOURLY, null, null, 8, 180))
                .isEqualTo(90000);
        // 이용 단위 미정은 시간 단위로 계산한다 (프론트와 동일)
        assertThat(BookingPriceCalculator.calculate(30000, false, null, null, null, 8, 120))
                .isEqualTo(60000);
    }

    @Test
    void 시간_단위_공간은_초과_인원_요금도_시간에_비례한다() {
        // (30,000 + 2명 × 5,000) × 3시간
        assertThat(BookingPriceCalculator.calculate(30000, false, UsageUnit.HOURLY, 4, 5000, 6, 180))
                .isEqualTo(120000);
    }

    @Test
    void 기준_인원_이하면_초과_요금이_없다() {
        assertThat(BookingPriceCalculator.calculate(30000, false, UsageUnit.HOURLY, 4, 5000, 4, 60))
                .isEqualTo(30000);
        assertThat(BookingPriceCalculator.calculate(30000, false, UsageUnit.HOURLY, 4, 5000, 1, 60))
                .isEqualTo(30000);
        // 인원을 안 보낸 예전 요청
        assertThat(BookingPriceCalculator.calculate(30000, false, UsageUnit.HOURLY, 4, 5000, null, 60))
                .isEqualTo(30000);
    }

    @Test
    void 일_단위_공간은_시간과_무관하게_1일_요금에_초과_인원_요금을_더한다() {
        assertThat(BookingPriceCalculator.calculate(200000, false, UsageUnit.DAILY, null, null, 10, 480))
                .isEqualTo(200000);
        assertThat(BookingPriceCalculator.calculate(200000, false, UsageUnit.DAILY, 10, 10000, 13, 480))
                .isEqualTo(230000);
    }

    @Test
    void 가격_문의_공간은_0원이다() {
        assertThat(BookingPriceCalculator.calculate(null, true, UsageUnit.HOURLY, 4, 5000, 10, 180))
                .isZero();
    }

    @Test
    void 반_시간_단위는_원_단위로_반올림한다() {
        // 10,001 × 1.5 = 15,001.5 → 15,002
        assertThat(BookingPriceCalculator.calculate(10001, false, UsageUnit.HOURLY, null, null, 1, 90))
                .isEqualTo(15002);
    }
}
