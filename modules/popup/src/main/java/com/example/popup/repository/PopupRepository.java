package com.example.popup.repository;

import com.example.popup.entity.Popup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PopupRepository extends JpaRepository<Popup, Long> {

    boolean existsByMatchingId(Long matchingId);

    Optional<Popup> findById(Long id);

    List<Popup> findAllByOrderByCreatedAtDesc();

    List<Popup> findTop10ByOrderByLikeCountDescViewCountDescCreatedAtDesc();

    List<Popup> findAllBySellerIdOrderByCreatedAtDesc(String sellerId);

    List<Popup> findAllBySpaceIdOrderByCreatedAtDesc(Long spaceId);
}
