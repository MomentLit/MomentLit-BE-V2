package com.example.space.entity;

import com.example.space.global.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpaceBookingRulesTest {

    private Space newSpace(Integer capacity) {
        return Space.create(
                "host-1", "디아트 스튜디오", "소개", null, 1L, null, 10000, false,
                SpaceCategory.STUDIO, null, null, capacity, null, null, null, false
        );
    }

    @Test
    void 새_공간은_연속_예약_꺼짐_최소시간_없음_초과요금_없음으로_시작한다() {
        Space space = newSpace(10);

        assertThat(space.getAllowConsecutiveBooking()).isFalse();
        assertThat(space.getMinUsageHours()).isNull();
        assertThat(space.getBaseGuestCount()).isNull();
        assertThat(space.getExtraGuestFee()).isNull();
    }

    @Test
    void 연속_예약은_null이면_그대로_둔다() {
        Space space = newSpace(10);

        space.updateConsecutiveBooking(true);
        space.updateConsecutiveBooking(null);

        assertThat(space.getAllowConsecutiveBooking()).isTrue();
    }

    @Test
    void 최소_이용_시간은_0이면_지우고_null이면_그대로_둔다() {
        Space space = newSpace(10);

        space.updateMinUsageHours(2);
        space.updateMinUsageHours(null);
        assertThat(space.getMinUsageHours()).isEqualTo(2);

        space.updateMinUsageHours(0);
        assertThat(space.getMinUsageHours()).isNull();
    }

    @Test
    void 초과_요금은_기준_인원과_함께_설정한다() {
        Space space = newSpace(10);

        space.updateExtraGuestFee(4, 5000);

        assertThat(space.getBaseGuestCount()).isEqualTo(4);
        assertThat(space.getExtraGuestFee()).isEqualTo(5000);
    }

    @Test
    void 처음_설정할_때_한쪽만_보내면_거부한다() {
        Space space = newSpace(10);

        assertThatThrownBy(() -> space.updateExtraGuestFee(4, null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> space.updateExtraGuestFee(null, 5000)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void 설정된_뒤에는_한쪽만_바꿀_수_있다() {
        Space space = newSpace(10);
        space.updateExtraGuestFee(4, 5000);

        space.updateExtraGuestFee(null, 7000);

        assertThat(space.getBaseGuestCount()).isEqualTo(4);
        assertThat(space.getExtraGuestFee()).isEqualTo(7000);
    }

    @Test
    void 추가_요금_0은_기준_인원과_함께_지운다() {
        Space space = newSpace(10);
        space.updateExtraGuestFee(4, 5000);

        space.updateExtraGuestFee(null, 0);

        assertThat(space.getBaseGuestCount()).isNull();
        assertThat(space.getExtraGuestFee()).isNull();
    }

    @Test
    void 기준_인원이_최대_인원보다_많거나_0이면_거부한다() {
        Space space = newSpace(10);

        assertThatThrownBy(() -> space.updateExtraGuestFee(11, 5000)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> space.updateExtraGuestFee(0, 5000)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void 아무것도_보내지_않으면_기존_공간은_그대로다() {
        Space space = newSpace(10);

        space.updateExtraGuestFee(null, null);

        assertThat(space.getBaseGuestCount()).isNull();
        assertThat(space.getExtraGuestFee()).isNull();
    }
}
