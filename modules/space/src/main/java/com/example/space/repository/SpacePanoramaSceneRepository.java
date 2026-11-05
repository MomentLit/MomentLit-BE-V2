package com.example.space.repository;

import com.example.space.entity.SpacePanoramaScene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpacePanoramaSceneRepository extends JpaRepository<SpacePanoramaScene, Long> {

    List<SpacePanoramaScene> findAllBySpaceIdOrderBySortOrderAsc(Long spaceId);

    void deleteAllBySpaceId(Long spaceId);
}
