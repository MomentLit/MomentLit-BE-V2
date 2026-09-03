package com.example.space.repository;

import com.example.space.entity.SpaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpaceImageRepository extends JpaRepository<SpaceImage, Long> {

    List<SpaceImage> findAllBySpaceId(Long spaceId);

    void deleteAllBySpaceId(Long spaceId);
}
