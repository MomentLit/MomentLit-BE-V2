package com.example.popup.repository;

import com.example.popup.entity.Popup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PopupRepository extends JpaRepository<Popup, Long> {

    boolean existsByMatchingId(Long matchingId);

    Optional<Popup> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Popup p where p.id = :popupId")
    Optional<Popup> findByIdWithLock(@Param("popupId") Long popupId);

    List<Popup> findAllByOrderByCreatedAtDesc();

    List<Popup> findTop10ByOrderByLikeCountDescViewCountDescCreatedAtDesc();

    List<Popup> findAllBySellerIdOrderByCreatedAtDesc(String sellerId);

    List<Popup> findAllBySpaceIdOrderByCreatedAtDesc(Long spaceId);
}
