package com.example.popup.service;

import com.example.common.dto.PageResponse;
import com.example.matching.api.MatchingInternalApi;
import com.example.popup.dto.response.PopupListResponse;
import com.example.popup.dto.response.PopupListResponses;
import com.example.popup.entity.Popup;
import com.example.popup.global.exception.PopupNotFoundException;
import com.example.popup.repository.PopupRepository;
import com.example.space.api.SpaceInternalApi;
import com.example.space.dto.response.AddressResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 공간이 소프트 삭제된 팝업이 목록·상세를 깨뜨리지 않는지(2026/11/15 /popups 404 장애) 확인한다. */
@ExtendWith(MockitoExtension.class)
class PopupServiceTest {

    private static final Long ACTIVE_SPACE_ID = 10L;
    private static final Long DELETED_SPACE_ID = 36L;
    private static final AddressResponse ADDRESS =
            new AddressResponse("서울특별시", "성동구", "성수동", null, null, null, null, "SEOUL");

    @Mock
    private PopupRepository popupRepository;

    @Mock
    private MatchingInternalApi matchingApi;

    @Mock
    private SpaceInternalApi spaceApi;

    private PopupService popupService;

    @BeforeEach
    void setUp() {
        popupService = new PopupService(popupRepository, matchingApi, spaceApi);
    }

    @Test
    void getPopups_excludesPopupsOfDeletedSpacesAtQueryLevel() {
        Pageable pageable = PageRequest.of(0, 3);
        Popup popup = popup(2L, ACTIVE_SPACE_ID);
        when(spaceApi.getDeletedSpaceIds()).thenReturn(Set.of(DELETED_SPACE_ID));
        when(popupRepository.findAllBySpaceIdNotIn(Set.of(DELETED_SPACE_ID), pageable))
                .thenReturn(new PageImpl<>(List.of(popup), pageable, 1));
        when(spaceApi.getActiveSpaceAddresses(List.of(ACTIVE_SPACE_ID))).thenReturn(Map.of(ACTIVE_SPACE_ID, ADDRESS));

        PageResponse<PopupListResponse> response = popupService.getPopups(pageable);

        assertThat(response.content()).extracting(PopupListResponse::popupId).containsExactly(2L);
        assertThat(response.content().getFirst().address()).isEqualTo(ADDRESS);
        assertThat(response.totalElements()).isEqualTo(1);
        verify(popupRepository, never()).findAll(any(Pageable.class));
        verify(spaceApi, never()).getSpace(any());
    }

    @Test
    void getPopups_usesPlainQueryWhenNoSpaceIsDeleted() {
        Pageable pageable = PageRequest.of(0, 3);
        when(spaceApi.getDeletedSpaceIds()).thenReturn(Set.of());
        when(popupRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));
        when(spaceApi.getActiveSpaceAddresses(List.of())).thenReturn(Map.of());

        PageResponse<PopupListResponse> response = popupService.getPopups(pageable);

        assertThat(response.content()).isEmpty();
    }

    @Test
    void getMyPopups_skipsPopupWhoseSpaceHasNoActiveAddress() {
        when(popupRepository.findAllBySellerIdOrderByCreatedAtDesc("seller-1"))
                .thenReturn(List.of(popup(1L, DELETED_SPACE_ID), popup(2L, ACTIVE_SPACE_ID)));
        when(spaceApi.getActiveSpaceAddresses(List.of(DELETED_SPACE_ID, ACTIVE_SPACE_ID)))
                .thenReturn(Map.of(ACTIVE_SPACE_ID, ADDRESS));

        PopupListResponses response = popupService.getMyPopups("seller-1");

        assertThat(response.popups()).extracting(PopupListResponse::popupId).containsExactly(2L);
    }

    @Test
    void getPopup_treatsPopupOfDeletedSpaceAsNotFoundWithoutCountingView() {
        Popup popup = popup(1L, DELETED_SPACE_ID);
        when(popupRepository.findById(1L)).thenReturn(Optional.of(popup));
        when(spaceApi.isActiveSpace(DELETED_SPACE_ID)).thenReturn(false);

        assertThatThrownBy(() -> popupService.getPopup(1L)).isInstanceOf(PopupNotFoundException.class);

        assertThat(popup.getViewCount()).isZero();
        verify(spaceApi, never()).getSpace(any());
    }

    private Popup popup(Long id, Long spaceId) {
        Popup popup = Popup.create(
                id, spaceId, "seller-1", "팝업 " + id, "설명", null,
                LocalDateTime.of(2026, 11, 12, 10, 0), LocalDateTime.of(2026, 11, 20, 18, 0)
        );
        ReflectionTestUtils.setField(popup, "id", id);
        return popup;
    }
}
