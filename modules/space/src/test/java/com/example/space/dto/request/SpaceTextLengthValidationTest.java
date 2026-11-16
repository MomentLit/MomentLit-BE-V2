package com.example.space.dto.request;

import com.example.space.entity.SpaceCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SpaceTextLengthValidationTest {

    private static final String MAX = "가".repeat(1000);
    private static final String TOO_LONG = "가".repeat(1001);

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private AddressRequest address(String detailAddress) {
        return new AddressRequest("부산광역시", "강서구", "명지동", "가락대로 1", null, detailAddress, "46700");
    }

    private SpaceCreateRequest createRequest(String name, String floor, String parkingInfo, AddressRequest address,
                                             List<PanoramaSceneRequest> panoramaScenes) {
        return new SpaceCreateRequest(
                name, "소개", address, null, null, null, panoramaScenes, 10000, null, SpaceCategory.STUDIO,
                null, null, null, null, null, floor, parkingInfo, null, null, null, false,
                null, null, null, null
        );
    }

    private SpaceUpdateRequest updateRequest(String name, String floor, String parkingInfo, AddressRequest address,
                                             List<PanoramaSceneRequest> panoramaScenes) {
        return new SpaceUpdateRequest(
                name, null, address, null, null, null, null, panoramaScenes, null, null, null,
                null, null, null, null, null, floor, parkingInfo, null, null, null, null,
                null, null, null, null
        );
    }

    private List<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).toList();
    }

    @Test
    void 텍스트_칸은_1000자까지_허용한다() {
        List<PanoramaSceneRequest> scenes = List.of(new PanoramaSceneRequest("https://img/1.jpg", MAX, null));

        assertThat(validator.validate(createRequest(MAX, MAX, MAX, address(MAX), scenes))).isEmpty();
        assertThat(validator.validate(updateRequest(MAX, MAX, MAX, address(MAX), scenes))).isEmpty();
    }

    @Test
    void 등록_시_1000자를_넘으면_칸별_메시지로_거부한다() {
        assertThat(messages(validator.validate(createRequest(TOO_LONG, null, null, address(null), null))))
                .containsExactly("공간 이름은 구역 이름을 포함해 1000자 이하로 입력해 주세요.");
        assertThat(messages(validator.validate(createRequest("공간", TOO_LONG, null, address(null), null))))
                .containsExactly("층 정보는 1000자 이하로 입력해 주세요.");
        assertThat(messages(validator.validate(createRequest("공간", null, TOO_LONG, address(null), null))))
                .containsExactly("주차 정보는 1000자 이하로 입력해 주세요.");
    }

    @Test
    void 주소와_360도_장면_안쪽_칸도_검증한다() {
        List<PanoramaSceneRequest> scenes = List.of(new PanoramaSceneRequest("https://img/1.jpg", TOO_LONG, null));

        assertThat(messages(validator.validate(createRequest("공간", null, null, address(TOO_LONG), null))))
                .containsExactly("상세 주소는 1000자 이하로 입력해 주세요.");
        assertThat(messages(validator.validate(createRequest("공간", null, null, address(null), scenes))))
                .containsExactly("360도 장면 이름은 1000자 이하로 입력해 주세요.");
    }

    @Test
    void 수정_시에도_같은_제한을_건다() {
        List<PanoramaSceneRequest> scenes = List.of(new PanoramaSceneRequest("https://img/1.jpg", TOO_LONG, null));

        assertThat(messages(validator.validate(updateRequest(TOO_LONG, TOO_LONG, TOO_LONG, address(TOO_LONG), scenes))))
                .containsExactlyInAnyOrder(
                        "공간 이름은 구역 이름을 포함해 1000자 이하로 입력해 주세요.",
                        "층 정보는 1000자 이하로 입력해 주세요.",
                        "주차 정보는 1000자 이하로 입력해 주세요.",
                        "상세 주소는 1000자 이하로 입력해 주세요.",
                        "360도 장면 이름은 1000자 이하로 입력해 주세요."
                );
    }
}
