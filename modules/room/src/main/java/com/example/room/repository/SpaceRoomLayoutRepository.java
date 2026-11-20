package com.example.room.repository;

import com.example.room.entity.SpaceRoomLayout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpaceRoomLayoutRepository extends JpaRepository<SpaceRoomLayout, Long> {

    List<SpaceRoomLayout> findAllByRoomIdAndUserIdOrderByUpdatedAtDesc(Long roomId, String userId);

    long countByRoomIdAndUserId(Long roomId, String userId);

    Optional<SpaceRoomLayout> findByIdAndRoomIdAndUserId(Long id, Long roomId, String userId);

    void deleteAllByRoomId(Long roomId);
}
