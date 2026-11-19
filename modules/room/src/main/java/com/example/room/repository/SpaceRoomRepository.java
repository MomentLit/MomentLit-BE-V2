package com.example.room.repository;

import com.example.room.entity.SpaceRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpaceRoomRepository extends JpaRepository<SpaceRoom, Long> {

    Optional<SpaceRoom> findBySpaceId(Long spaceId);

    boolean existsBySpaceId(Long spaceId);
}
