package com.example.space.entity;

import com.example.space.global.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpaceBookingTest {

    private Space newSpace() {
        return Space.create(
                "host-1", "디아트 스튜디오", "소개", null, 1L, null, 10000,
                SpaceCategory.STUDIO, null, null, null, null, null, null, false
        );
    }

    @Test
    void 기본_예약_방식은_INTERNAL이다() {
        Space space = newSpace();

        assertThat(space.getBookingType()).isEqualTo(BookingType.INTERNAL);
        assertThat(space.getExternalBookingUrl()).isNull();
        assertThat(space.isExternalBooking()).isFalse();
    }

    @Test
    void EXTERNAL로_바꾸면_링크가_저장된다() {
        Space space = newSpace();

        space.updateBooking(BookingType.EXTERNAL, "  https://diart.example.com/booking  ");

        assertThat(space.isExternalBooking()).isTrue();
        assertThat(space.getExternalBookingUrl()).isEqualTo("https://diart.example.com/booking");
    }

    @Test
    void EXTERNAL인데_링크가_없으면_거부한다() {
        Space space = newSpace();

        assertThatThrownBy(() -> space.updateBooking(BookingType.EXTERNAL, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> space.updateBooking(BookingType.EXTERNAL, " "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void http_https가_아닌_링크는_거부한다() {
        Space space = newSpace();

        assertThatThrownBy(() -> space.updateBooking(BookingType.EXTERNAL, "javascript:alert(1)"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> space.updateBooking(BookingType.EXTERNAL, "diart.example.com"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void INTERNAL로_되돌리면_링크를_비운다() {
        Space space = newSpace();
        space.updateBooking(BookingType.EXTERNAL, "https://diart.example.com");

        space.updateBooking(BookingType.INTERNAL, null);

        assertThat(space.isExternalBooking()).isFalse();
        assertThat(space.getExternalBookingUrl()).isNull();
    }

    @Test
    void 타입_없이_링크만_보내면_기존_EXTERNAL_링크를_교체한다() {
        Space space = newSpace();
        space.updateBooking(BookingType.EXTERNAL, "https://old.example.com");

        space.updateBooking(null, "https://new.example.com");

        assertThat(space.getExternalBookingUrl()).isEqualTo("https://new.example.com");
    }

    @Test
    void EXTERNAL_공간의_링크를_빈_문자열로_지우면_거부한다() {
        Space space = newSpace();
        space.updateBooking(BookingType.EXTERNAL, "https://diart.example.com");

        assertThatThrownBy(() -> space.updateBooking(null, ""))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 둘_다_null이면_변경하지_않는다() {
        Space space = newSpace();
        space.updateBooking(BookingType.EXTERNAL, "https://diart.example.com");

        space.updateBooking(null, null);

        assertThat(space.getExternalBookingUrl()).isEqualTo("https://diart.example.com");
    }
}
