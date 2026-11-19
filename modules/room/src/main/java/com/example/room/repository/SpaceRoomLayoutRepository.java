package com.example.room.repository;

import com.example.room.entity.SpaceRoomLayout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpaceRoomLayoutRepository extends JpaRepository<SpaceRoomLayout, Long> {

    List<SpaceRoomLayout> findAllBySpaceIdAndUserIdOrderByUpdatedAtDesc(Long spaceId, String userId);

    long countBySpaceIdAndUserId(Long spaceId, String userId);

    Optional<SpaceRoomLayout> findByIdAndSpaceIdAndUserId(Long id, Long spaceId, String userId);
}
