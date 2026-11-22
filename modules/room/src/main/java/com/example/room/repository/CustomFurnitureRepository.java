package com.example.room.repository;

import com.example.room.entity.CustomFurniture;
import com.example.room.entity.CustomFurnitureSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomFurnitureRepository extends JpaRepository<CustomFurniture, Long> {

    List<CustomFurniture> findAllByUserIdOrderByCreatedAtDesc(String userId);

    Optional<CustomFurniture> findByIdAndUserId(Long id, String userId);

    long countByUserId(String userId);

    long countByUserIdAndSourceAndCreatedAtAfter(String userId, CustomFurnitureSource source, LocalDateTime after);
}
