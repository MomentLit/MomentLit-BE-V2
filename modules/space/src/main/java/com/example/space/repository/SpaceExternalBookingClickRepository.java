package com.example.space.repository;

import com.example.space.entity.SpaceExternalBookingClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SpaceExternalBookingClickRepository extends JpaRepository<SpaceExternalBookingClick, Long> {

    long countBySpaceId(Long spaceId);

    long countBySpaceIdAndCreatedAtGreaterThanEqual(Long spaceId, LocalDateTime from);
}
