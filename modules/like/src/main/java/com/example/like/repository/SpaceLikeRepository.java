package com.example.like.repository;

import com.example.like.entity.SpaceLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpaceLikeRepository extends JpaRepository<SpaceLike, Long> {

    boolean existsBySpaceIdAndUserId(Long spaceId, String userId);

    Optional<SpaceLike> findBySpaceIdAndUserId(Long spaceId, String userId);
}
