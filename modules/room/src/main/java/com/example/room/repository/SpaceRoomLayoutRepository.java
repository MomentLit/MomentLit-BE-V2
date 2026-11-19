package com.example.room.repository;

import com.example.room.entity.SpaceRoomLayout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpaceRoomLayoutRepository extends JpaRepository<SpaceRoomLayout, Long> {

    Optional<SpaceRoomLayout> findBySpaceIdAndUserId(Long spaceId, String userId);
}
