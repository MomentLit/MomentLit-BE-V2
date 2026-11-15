package com.example.popup.repository;

import com.example.popup.entity.Popup;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PopupRepository extends JpaRepository<Popup, Long> {

    boolean existsByMatchingId(Long matchingId);

    Optional<Popup> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Popup p where p.id = :popupId")
    Optional<Popup> findByIdWithLock(@Param("popupId") Long popupId);

    List<Popup> findTop10ByOrderByLikeCountDescViewCountDescCreatedAtDesc();

    // 공간이 삭제된 팝업을 조회 단계에서 빼는 버전 — 페이지 크기·총 개수가 실제 노출 건수와 맞도록.
    // (빈 컬렉션의 NOT IN은 DB마다 동작이 달라 호출부에서 비어 있으면 위의 기본 메서드를 쓴다.)
    Page<Popup> findAllBySpaceIdNotIn(Collection<Long> spaceIds, Pageable pageable);

    List<Popup> findTop10BySpaceIdNotInOrderByLikeCountDescViewCountDescCreatedAtDesc(Collection<Long> spaceIds);

    List<Popup> findAllBySellerIdOrderByCreatedAtDesc(String sellerId);

    List<Popup> findAllBySpaceIdOrderByCreatedAtDesc(Long spaceId);
}
