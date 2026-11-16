package com.example.space.entity;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpaceAvailabilityConsecutiveTest {

    private static SpaceAvailability slot(String start, String end) {
        return SpaceAvailability.create(1L, DayOfWeek.MONDAY, LocalTime.parse(start), LocalTime.parse(end), true);
    }

    // 10:00~18:00을 2시간 단위로 연 경우 (등록 화면이 만드는 모양)
    private final List<SpaceAvailability> twoHourSlots = List.of(
            slot("14:00", "16:00"), slot("10:00", "12:00"), slot("16:00", "18:00"), slot("12:00", "14:00")
    );

    private boolean covers(List<SpaceAvailability> slots, String start, String end) {
        return SpaceAvailability.coversConsecutively(slots, LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    void 이어진_슬롯들로_요청_구간을_덮으면_true() {
        assertThat(covers(twoHourSlots, "10:00", "14:00")).isTrue();
        assertThat(covers(twoHourSlots, "12:00", "18:00")).isTrue();
        assertThat(covers(twoHourSlots, "10:00", "18:00")).isTrue();
        assertThat(covers(twoHourSlots, "14:00", "16:00")).isTrue();
    }

    @Test
    void 슬롯_경계와_어긋나면_false() {
        assertThat(covers(twoHourSlots, "11:00", "14:00")).isFalse();
        assertThat(covers(twoHourSlots, "10:00", "13:00")).isFalse();
    }

    @Test
    void 운영_시간을_벗어나면_false() {
        assertThat(covers(twoHourSlots, "08:00", "12:00")).isFalse();
        assertThat(covers(twoHourSlots, "16:00", "20:00")).isFalse();
    }

    @Test
    void 중간에_빈_시간이_있으면_false() {
        List<SpaceAvailability> withGap = List.of(slot("10:00", "12:00"), slot("13:00", "15:00"));

        assertThat(covers(withGap, "10:00", "15:00")).isFalse();
        assertThat(covers(withGap, "13:00", "15:00")).isTrue();
    }

    @Test
    void 슬롯이_없으면_false() {
        assertThat(covers(List.of(), "10:00", "12:00")).isFalse();
    }
}
