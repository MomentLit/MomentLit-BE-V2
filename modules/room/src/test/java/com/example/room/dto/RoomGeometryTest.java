package com.example.room.dto;

import com.example.room.dto.request.RoomGeometry;
import com.example.room.global.exception.InvalidRoomException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomGeometryTest {

    private static List<List<Double>> rect(double w, double d) {
        return List.of(List.of(0.0, 0.0), List.of(w, 0.0), List.of(w, d), List.of(0.0, d));
    }

    private static RoomGeometry outerOnly(List<List<Double>> outer) {
        return new RoomGeometry(outer, null, null, null, null);
    }

    @Test
    void 직사각형_면적을_계산한다() {
        assertThat(outerOnly(rect(4, 3)).validateAndComputeArea()).isEqualTo(12.0);
    }

    @Test
    void 꼭짓점_순서가_반대여도_면적은_양수다() {
        List<List<Double>> clockwise = List.of(List.of(0.0, 0.0), List.of(0.0, 3.0), List.of(4.0, 3.0), List.of(4.0, 0.0));
        assertThat(outerOnly(clockwise).validateAndComputeArea()).isEqualTo(12.0);
    }

    @Test
    void L자_방_면적을_계산한다() {
        // 4×4에서 오른쪽 위 2×2를 뺀 L자 = 12㎡
        List<List<Double>> l = List.of(
                List.of(0.0, 0.0), List.of(4.0, 0.0), List.of(4.0, 2.0),
                List.of(2.0, 2.0), List.of(2.0, 4.0), List.of(0.0, 4.0));
        assertThat(outerOnly(l).validateAndComputeArea()).isEqualTo(12.0);
    }

    @Test
    void 꼭짓점이_3개_미만이면_거절한다() {
        assertThatThrownBy(() -> outerOnly(List.of(List.of(0.0, 0.0), List.of(1.0, 0.0))).validateAndComputeArea())
                .isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void 한_줄로_놓인_꼭짓점은_면적이_없어_거절한다() {
        List<List<Double>> line = List.of(List.of(0.0, 0.0), List.of(1.0, 0.0), List.of(2.0, 0.0));
        assertThatThrownBy(() -> outerOnly(line).validateAndComputeArea())
                .isInstanceOf(InvalidRoomException.class)
                .hasMessageContaining("면적");
    }

    @Test
    void 좌표가_100m를_넘으면_거절한다_mm_단위_실수_방지() {
        assertThatThrownBy(() -> outerOnly(rect(4000, 3000)).validateAndComputeArea())
                .isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void 좌표가_두_값이_아니면_거절한다() {
        List<List<Double>> bad = List.of(List.of(0.0, 0.0), List.of(1.0, 0.0, 5.0), List.of(1.0, 1.0));
        assertThatThrownBy(() -> outerOnly(bad).validateAndComputeArea())
                .isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void 안쪽_벽은_끝점_두_개여야_한다() {
        RoomGeometry g = new RoomGeometry(rect(4, 3), List.of(List.of(List.of(1.0, 1.0))), null, null, null);
        assertThatThrownBy(g::validateAndComputeArea).isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void 모서리_uv는_0에서_1_사이여야_한다() {
        RoomGeometry g = new RoomGeometry(rect(4, 3), null, List.of(1.0, 1.0), 0.0, List.of(List.of(0.5, 1.2)));
        assertThatThrownBy(g::validateAndComputeArea).isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void 파노라마_방_전체_구조가_올바르면_통과한다() {
        RoomGeometry g = new RoomGeometry(
                rect(4, 3),
                List.of(List.of(List.of(2.0, 0.0), List.of(2.0, 1.5))),
                List.of(2.0, 1.5),
                0.3,
                List.of(List.of(0.1, 0.3), List.of(0.1, 0.7)));
        assertThat(g.validateAndComputeArea()).isEqualTo(12.0);
    }
}
