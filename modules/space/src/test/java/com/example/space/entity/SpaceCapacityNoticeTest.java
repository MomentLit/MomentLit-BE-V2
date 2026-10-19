package com.example.space.entity;

import com.example.space.global.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpaceCapacityNoticeTest {

    private Space newSpace(Integer capacity, String phone) {
        return Space.create(
                "host-1", "디아트 스튜디오", "소개", null, 1L, null, 10000,
                SpaceCategory.STUDIO, phone, null, capacity, null, null, null, false
        );
    }

    @Test
    void 최소_인원을_최대_인원_이하로_설정한다() {
        Space space = newSpace(10, null);

        space.updateMinCapacity(2);

        assertThat(space.getMinCapacity()).isEqualTo(2);
    }

    @Test
    void 최소_인원이_최대_인원보다_많으면_거부한다() {
        Space space = newSpace(10, null);

        assertThatThrownBy(() -> space.updateMinCapacity(11))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 최소_인원_0은_삭제_null은_변경_없음() {
        Space space = newSpace(10, null);
        space.updateMinCapacity(3);

        space.updateMinCapacity(null);
        assertThat(space.getMinCapacity()).isEqualTo(3);

        space.updateMinCapacity(0);
        assertThat(space.getMinCapacity()).isNull();
    }

    @Test
    void 최대_인원이_없으면_최소_인원만_둘_수_있다() {
        Space space = newSpace(null, null);

        space.updateMinCapacity(5);

        assertThat(space.getMinCapacity()).isEqualTo(5);
    }

    @Test
    void 이용_안내는_빈_문자열이면_지우고_null이면_그대로_둔다() {
        Space space = newSpace(null, null);

        space.updateUsageNotice("  주차 불가 / 문의는 채팅으로  ");
        assertThat(space.getUsageNotice()).isEqualTo("주차 불가 / 문의는 채팅으로");

        space.updateUsageNotice(null);
        assertThat(space.getUsageNotice()).isEqualTo("주차 불가 / 문의는 채팅으로");

        space.updateUsageNotice(" ");
        assertThat(space.getUsageNotice()).isNull();
    }

    @Test
    void 빈_연락처는_저장하지_않는다() {
        assertThat(newSpace(null, "").getPhone()).isNull();
        assertThat(newSpace(null, " 02-123-4567 ").getPhone()).isEqualTo("02-123-4567");
    }
}
