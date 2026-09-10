package com.example.space.repository;

import com.example.space.entity.ApprovalStatus;
import com.example.space.entity.Space;
import com.example.space.entity.SpaceCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpaceRepository extends JpaRepository<Space, Long> {

    Optional<Space> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Space s where s.id = :spaceId and s.deletedAt is null")
    Optional<Space> findByIdWithLock(@Param("spaceId") Long spaceId);

    List<Space> findAllByDeletedAtIsNull();

    List<Space> findAllByHostIdAndDeletedAtIsNull(String hostId);

    List<Space> findAllByNameContainingAndDeletedAtIsNull(String name);

    List<Space> findAllByCategoryAndDeletedAtIsNull(SpaceCategory category);

    List<Space> findAllByNameContainingAndCategoryAndDeletedAtIsNull(
            String name,
            SpaceCategory category
    );

    List<Space> findAllByAdminStatusAndIsActiveTrueAndDeletedAtIsNull(
            ApprovalStatus adminStatus
    );

    List<Space> findAllByHostIdAndNameContainingAndDeletedAtIsNull(
            String hostId,
            String name
    );

    List<Space> findAllByHostIdAndCategoryAndDeletedAtIsNull(
            String hostId,
            SpaceCategory category
    );

    List<Space> findAllByHostIdAndNameContainingAndCategoryAndDeletedAtIsNull(
            String hostId,
            String name,
            SpaceCategory category
    );
}
