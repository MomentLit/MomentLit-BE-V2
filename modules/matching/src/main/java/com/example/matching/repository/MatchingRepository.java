package com.example.matching.repository;

import com.example.matching.entity.Matching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;

@Repository
public interface MatchingRepository extends JpaRepository<Matching, Long> {

    List<Matching> findBySellerIdOrderByCreatedAtDesc(String sellerId);

    List<Matching> findByHostIdOrderByCreatedAtDesc(String hostId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Matching m where m.spaceId = :spaceId order by m.id")
    List<Matching> findAllBySpaceIdForUpdate(@Param("spaceId") Long spaceId);
}
