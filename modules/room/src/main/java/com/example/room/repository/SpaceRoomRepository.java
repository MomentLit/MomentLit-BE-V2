package com.example.room.repository;

import com.example.room.entity.SpaceRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpaceRoomRepository extends JpaRepository<SpaceRoom, Long> {

    List<SpaceRoom> findAllBySpaceIdOrderBySortOrderAscIdAsc(Long spaceId);

    Optional<SpaceRoom> findByIdAndSpaceId(Long id, Long spaceId);

    long countBySpaceId(Long spaceId);
}
