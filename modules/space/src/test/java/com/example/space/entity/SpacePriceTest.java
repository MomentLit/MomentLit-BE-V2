package com.example.space.entity;

import com.example.space.global.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpacePriceTest {

    private Space newSpace(Integer pricePerHour, Boolean priceInquiry) {
        return Space.create(
                "host-1", "디아트 스튜디오", "소개", null, 1L, null, pricePerHour, priceInquiry,
                SpaceCategory.STUDIO, null, null, null, null, null, null, false
        );
    }

    @Test
    void 가격_문의를_지정하지_않으면_일반_가격_공간이다() {
        Space space = newSpace(10000, null);

        assertThat(space.getPriceInquiry()).isFalse();
        assertThat(space.getPricePerHour()).isEqualTo(10000);
    }

    @Test
    void 가격_문의가_아니면_가격이_없거나_양수가_아니면_거부한다() {
        assertThatThrownBy(() -> newSpace(null, null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> newSpace(null, false)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> newSpace(0, false)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> newSpace(-1000, null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void 가격_문의_공간은_가격을_보내도_무시하고_비운다() {
        assertThat(newSpace(null, true).getPricePerHour()).isNull();

        Space space = newSpace(0, true);

        assertThat(space.getPriceInquiry()).isTrue();
        assertThat(space.getPricePerHour()).isNull();
    }

    @Test
    void 수정할_값이_없으면_그대로_둔다() {
        Space space = newSpace(10000, false);

        space.updatePrice(null, null);

        assertThat(space.getPriceInquiry()).isFalse();
        assertThat(space.getPricePerHour()).isEqualTo(10000);
    }

    @Test
    void 가격만_수정하면_가격_문의_여부는_유지된다() {
        Space space = newSpace(10000, false);

        space.updatePrice(null, 20000);

        assertThat(space.getPricePerHour()).isEqualTo(20000);

        assertThatThrownBy(() -> space.updatePrice(null, 0)).isInstanceOf(BadRequestException.class);
        assertThat(space.getPricePerHour()).isEqualTo(20000);
    }

    @Test
    void 가격_문의로_바꾸면_가격을_지운다() {
        Space space = newSpace(10000, false);

        space.updatePrice(true, null);

        assertThat(space.getPriceInquiry()).isTrue();
        assertThat(space.getPricePerHour()).isNull();
    }

    @Test
    void 가격_문의_공간에_가격만_보내면_무시한다() {
        Space space = newSpace(null, true);

        space.updatePrice(null, 20000);

        assertThat(space.getPriceInquiry()).isTrue();
        assertThat(space.getPricePerHour()).isNull();
    }

    @Test
    void 가격_문의를_해제할_때_가격이_없으면_거부한다() {
        Space space = newSpace(null, true);

        assertThatThrownBy(() -> space.updatePrice(false, null))
                .isInstanceOf(BadRequestException.class);
        assertThat(space.getPriceInquiry()).isTrue();
    }

    @Test
    void 가격_문의를_해제하면서_가격을_보내면_일반_공간이_된다() {
        Space space = newSpace(null, true);

        space.updatePrice(false, 15000);

        assertThat(space.getPriceInquiry()).isFalse();
        assertThat(space.getPricePerHour()).isEqualTo(15000);
    }

    @Test
    void 이미_일반_공간이면_가격_없이_false를_보내도_기존_가격을_유지한다() {
        Space space = newSpace(10000, false);

        space.updatePrice(false, null);

        assertThat(space.getPriceInquiry()).isFalse();
        assertThat(space.getPricePerHour()).isEqualTo(10000);
    }
}
