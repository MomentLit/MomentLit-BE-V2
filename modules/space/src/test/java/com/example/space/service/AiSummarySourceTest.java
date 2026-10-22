package com.example.space.service;

import com.example.space.dto.request.SpaceUpdateRequest;
import com.example.space.entity.Address;
import com.example.space.entity.Space;
import com.example.space.entity.SpaceCategory;
import com.example.space.repository.AddressRepository;
import com.example.space.repository.AiSummaryOutboxRepository;
import com.example.space.repository.SpaceRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiSummarySourceTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void 이용안내만_수정하거나_삭제해도_요약을_재생성한다() {
        for (String notice : new String[]{"벽면 타공 불가", "", " "}) {
            String json = mapper.writeValueAsString(java.util.Map.of("usage_notice", notice));
            SpaceUpdateRequest request = mapper.readValue(json, SpaceUpdateRequest.class);

            assertThat(request.hasAiSummarySourceChanges()).isTrue();
        }

        assertThat(mapper.readValue("{}", SpaceUpdateRequest.class).hasAiSummarySourceChanges()).isFalse();
        assertThat(mapper.readValue("{\"usage_notice\":null}", SpaceUpdateRequest.class)
                .hasAiSummarySourceChanges()).isFalse();
    }

    @Test
    void 저장된_소개와_이용안내와_최소인원을_요약요청_JSON으로_전달한다() {
        SpaceRepository spaces = mock(SpaceRepository.class);
        AddressRepository addresses = mock(AddressRepository.class);
        AiSummaryOutboxService service = new AiSummaryOutboxService(
                mock(AiSummaryOutboxRepository.class), spaces, addresses);
        Space space = Space.create("host-1", "공간", "창가 테이블 구역", null, 1L, null,
                20000, SpaceCategory.CAFE, null, null, 15, "1층", null, null, false);
        space.updateUsageNotice("벽면 타공 불가, 음식물 반입은 사전 협의");
        space.updateMinCapacity(3);
        when(spaces.findById(1L)).thenReturn(Optional.of(space));
        when(addresses.findById(1L)).thenReturn(Optional.of(
                Address.create("부산광역시", "부산진구", null, "도로명", null, null, "12345")));
        var job = new AiSummaryOutboxService.AiSummaryJob("event", 1L, space.getAiSummaryVersion());

        var request = service.loadRequest(job).orElseThrow();
        var json = mapper.readTree(mapper.writeValueAsString(request));

        assertThat(json.get("description").asText()).isEqualTo("창가 테이블 구역");
        assertThat(json.get("usage_notice").asText()).isEqualTo("벽면 타공 불가, 음식물 반입은 사전 협의");
        assertThat(json.get("min_capacity").asInt()).isEqualTo(3);
        assertThat(json.get("capacity").asInt()).isEqualTo(15);

        space.updateUsageNotice("");
        assertThat(service.loadRequest(job).orElseThrow().usageNotice()).isNull();
    }
}
