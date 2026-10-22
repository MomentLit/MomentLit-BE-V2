package com.example.space.repository;

import com.example.space.entity.SpacePanoramaSceneLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpacePanoramaSceneLinkRepository extends JpaRepository<SpacePanoramaSceneLink, Long> {

    List<SpacePanoramaSceneLink> findAllBySpaceId(Long spaceId);

    void deleteAllBySpaceId(Long spaceId);
}
