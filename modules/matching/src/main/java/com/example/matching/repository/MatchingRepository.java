package com.example.matching.repository;

import com.example.matching.entity.Matching;
import com.example.matching.entity.MatchingStatus;
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

    /**
     * 호스트 응답률/평균 응답시간 집계용 쿼리.
     * 모든 매칭은 생성 시 REQUESTED로 시작하므로, hostId 기준 전체 건수가
     * "REQUESTED 상태를 거친 전체 매칭 수"에 해당한다.
     */
    @Query("SELECT COUNT(m) FROM Matching m WHERE m.hostId = :hostId")
    long countByHostId(@Param("hostId") String hostId);

    @Query("SELECT COUNT(m) FROM Matching m WHERE m.hostId = :hostId AND m.status IN :statuses")
    long countByHostIdAndStatusIn(
            @Param("hostId") String hostId,
            @Param("statuses") List<MatchingStatus> statuses
    );

    List<Matching> findByHostIdAndStatusIn(String hostId, List<MatchingStatus> statuses);
}
