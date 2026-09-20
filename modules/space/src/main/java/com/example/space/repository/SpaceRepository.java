package com.example.space.repository;

import com.example.space.entity.ApprovalStatus;
import com.example.space.entity.Region;
import com.example.space.entity.Space;
import com.example.space.entity.SpaceCategory;
import com.example.space.entity.UsageUnit;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpaceRepository extends JpaRepository<Space, Long> {

    Optional<Space> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Space s where s.id = :spaceId and s.deletedAt is null")
    Optional<Space> findByIdWithLock(@Param("spaceId") Long spaceId);

    List<Space> findAllByDeletedAtIsNull();

    List<Space> findByIdInAndDeletedAtIsNull(List<Long> ids);

    /**
     * 공개 검색용 조건부 필터 + 페이지네이션 조회.
     * Address와는 JPA 연관관계가 없으므로(전 프로젝트 컨벤션) theta join(콤마 조인)으로 region을 필터링한다.
     * 정렬은 서비스 계층에서 JpaSort.unsafe로 안전하게 가공된 Pageable을 전달받아 사용한다.
     * (좌표 데이터가 없어 거리순 정렬은 미지원)
     * dayOfWeek는 특정 날짜 검색을 위한 것 — 그 요일에 열려 있는 주간 반복 가용시간(SpaceAvailability)이
     * 하나라도 있는 공간만 남긴다. date는 그 날짜에 이미 승인된 예약으로 찬(SpaceBookedDate) 공간을
     * 추가로 제외한다 — 요일상 정기적으로 열려 있어도 그 특정 날짜엔 예약이 꽉 찼을 수 있어 둘 다 본다.
     */
    @Query(
            value = "SELECT s FROM Space s, Address a "
                    + "WHERE s.addressId = a.id "
                    + "AND s.deletedAt IS NULL "
                    // 공개 검색은 승인·활성 상태인 공간만 노출한다 — PENDING/REJECTED/DRAFT나
                    // 비활성 공간이 검색에 잡히면 등록 마법사의 "검토 후 공개됩니다" 안내와
                    // 모순되고, 승인 전 공간이 예약까지 가능한 것처럼 보이는 버그로 이어진다.
                    + "AND s.isActive = true "
                    + "AND s.adminStatus = com.example.space.entity.ApprovalStatus.APPROVED "
                    + "AND (:namePattern IS NULL OR s.name LIKE :namePattern) "
                    + "AND (:category IS NULL OR s.category = :category) "
                    + "AND (:region IS NULL OR a.region = :region) "
                    + "AND (:usageUnit IS NULL OR s.usageUnit = :usageUnit) "
                    + "AND (:minCapacity IS NULL OR s.capacity >= :minCapacity) "
                    + "AND (:maxCapacity IS NULL OR s.capacity <= :maxCapacity) "
                    + "AND (:dayOfWeek IS NULL OR EXISTS ("
                    + "  SELECT 1 FROM SpaceAvailability sa "
                    + "  WHERE sa.spaceId = s.id AND sa.dayOfWeek = :dayOfWeek AND sa.isOpen = true"
                    + ")) "
                    // ":date IS NULL"만 단독으로 두면 Postgres가 그 파라미터의 타입을 추론하지
                    // 못해 "could not determine data type of parameter" 에러가 난다(:namePattern에
                    //썼던 CONCAT 버그와 같은 종류). 그래서 별도의 boolean 플래그로 분기하고,
                    // :date는 항상 타입이 분명한 bd.date 비교 자리에서만 한 번 쓴다.
                    + "AND (:dateProvided = false OR NOT EXISTS ("
                    + "  SELECT 1 FROM SpaceBookedDate bd "
                    + "  WHERE bd.spaceId = s.id AND bd.date = :date"
                    + "))",
            countQuery = "SELECT COUNT(s) FROM Space s, Address a "
                    + "WHERE s.addressId = a.id "
                    + "AND s.deletedAt IS NULL "
                    + "AND s.isActive = true "
                    + "AND s.adminStatus = com.example.space.entity.ApprovalStatus.APPROVED "
                    + "AND (:namePattern IS NULL OR s.name LIKE :namePattern) "
                    + "AND (:category IS NULL OR s.category = :category) "
                    + "AND (:region IS NULL OR a.region = :region) "
                    + "AND (:usageUnit IS NULL OR s.usageUnit = :usageUnit) "
                    + "AND (:minCapacity IS NULL OR s.capacity >= :minCapacity) "
                    + "AND (:maxCapacity IS NULL OR s.capacity <= :maxCapacity) "
                    + "AND (:dayOfWeek IS NULL OR EXISTS ("
                    + "  SELECT 1 FROM SpaceAvailability sa "
                    + "  WHERE sa.spaceId = s.id AND sa.dayOfWeek = :dayOfWeek AND sa.isOpen = true"
                    + ")) "
                    + "AND (:dateProvided = false OR NOT EXISTS ("
                    + "  SELECT 1 FROM SpaceBookedDate bd "
                    + "  WHERE bd.spaceId = s.id AND bd.date = :date"
                    + "))"
    )
    Page<Space> searchSpaces(
            @Param("namePattern") String namePattern,
            @Param("category") SpaceCategory category,
            @Param("region") Region region,
            @Param("usageUnit") UsageUnit usageUnit,
            @Param("minCapacity") Integer minCapacity,
            @Param("maxCapacity") Integer maxCapacity,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("dateProvided") boolean dateProvided,
            @Param("date") LocalDate date,
            Pageable pageable
    );

    /**
     * searchSpaces와 필터는 동일하되, 정렬만 (:lat, :lng)로부터의 거리 오름차순으로 고정한다.
     * 진짜 지오코딩이 없어 위경도가 권역 단위 근사치라, 제곱거리(= 거리 순서와 단조 동치이며
     * SQRT를 안 써도 되는) 기준으로 비교한다 — km 단위로 쓸 값이 아니라 순서만 필요해서다.
     */
    @Query(
            value = "SELECT s FROM Space s, Address a "
                    + "WHERE s.addressId = a.id "
                    + "AND s.deletedAt IS NULL "
                    + "AND s.isActive = true "
                    + "AND s.adminStatus = com.example.space.entity.ApprovalStatus.APPROVED "
                    + "AND (:namePattern IS NULL OR s.name LIKE :namePattern) "
                    + "AND (:category IS NULL OR s.category = :category) "
                    + "AND (:region IS NULL OR a.region = :region) "
                    + "AND (:usageUnit IS NULL OR s.usageUnit = :usageUnit) "
                    + "AND (:minCapacity IS NULL OR s.capacity >= :minCapacity) "
                    + "AND (:maxCapacity IS NULL OR s.capacity <= :maxCapacity) "
                    + "AND (:dayOfWeek IS NULL OR EXISTS ("
                    + "  SELECT 1 FROM SpaceAvailability sa "
                    + "  WHERE sa.spaceId = s.id AND sa.dayOfWeek = :dayOfWeek AND sa.isOpen = true"
                    + ")) "
                    + "AND (:dateProvided = false OR NOT EXISTS ("
                    + "  SELECT 1 FROM SpaceBookedDate bd "
                    + "  WHERE bd.spaceId = s.id AND bd.date = :date"
                    + ")) "
                    + "ORDER BY (a.latitude - :lat) * (a.latitude - :lat) + (a.longitude - :lng) * (a.longitude - :lng) ASC",
            countQuery = "SELECT COUNT(s) FROM Space s, Address a "
                    + "WHERE s.addressId = a.id "
                    + "AND s.deletedAt IS NULL "
                    + "AND s.isActive = true "
                    + "AND s.adminStatus = com.example.space.entity.ApprovalStatus.APPROVED "
                    + "AND (:namePattern IS NULL OR s.name LIKE :namePattern) "
                    + "AND (:category IS NULL OR s.category = :category) "
                    + "AND (:region IS NULL OR a.region = :region) "
                    + "AND (:usageUnit IS NULL OR s.usageUnit = :usageUnit) "
                    + "AND (:minCapacity IS NULL OR s.capacity >= :minCapacity) "
                    + "AND (:maxCapacity IS NULL OR s.capacity <= :maxCapacity) "
                    + "AND (:dayOfWeek IS NULL OR EXISTS ("
                    + "  SELECT 1 FROM SpaceAvailability sa "
                    + "  WHERE sa.spaceId = s.id AND sa.dayOfWeek = :dayOfWeek AND sa.isOpen = true"
                    + ")) "
                    + "AND (:dateProvided = false OR NOT EXISTS ("
                    + "  SELECT 1 FROM SpaceBookedDate bd "
                    + "  WHERE bd.spaceId = s.id AND bd.date = :date"
                    + "))"
    )
    Page<Space> searchSpacesByDistance(
            @Param("namePattern") String namePattern,
            @Param("category") SpaceCategory category,
            @Param("region") Region region,
            @Param("usageUnit") UsageUnit usageUnit,
            @Param("minCapacity") Integer minCapacity,
            @Param("maxCapacity") Integer maxCapacity,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("dateProvided") boolean dateProvided,
            @Param("date") LocalDate date,
            @Param("lat") double lat,
            @Param("lng") double lng,
            Pageable pageable
    );

    /**
     * 내 공간 목록용 조건부 필터 + 페이지네이션 조회. (searchSpaces와 동일한 필터 + hostId 고정)
     */
    @Query(
            value = "SELECT s FROM Space s, Address a "
                    + "WHERE s.addressId = a.id "
                    + "AND s.hostId = :hostId "
                    + "AND s.deletedAt IS NULL "
                    + "AND (:namePattern IS NULL OR s.name LIKE :namePattern) "
                    + "AND (:category IS NULL OR s.category = :category) "
                    + "AND (:region IS NULL OR a.region = :region) "
                    + "AND (:usageUnit IS NULL OR s.usageUnit = :usageUnit) "
                    + "AND (:minCapacity IS NULL OR s.capacity >= :minCapacity) "
                    + "AND (:maxCapacity IS NULL OR s.capacity <= :maxCapacity)",
            countQuery = "SELECT COUNT(s) FROM Space s, Address a "
                    + "WHERE s.addressId = a.id "
                    + "AND s.hostId = :hostId "
                    + "AND s.deletedAt IS NULL "
                    + "AND (:namePattern IS NULL OR s.name LIKE :namePattern) "
                    + "AND (:category IS NULL OR s.category = :category) "
                    + "AND (:region IS NULL OR a.region = :region) "
                    + "AND (:usageUnit IS NULL OR s.usageUnit = :usageUnit) "
                    + "AND (:minCapacity IS NULL OR s.capacity >= :minCapacity) "
                    + "AND (:maxCapacity IS NULL OR s.capacity <= :maxCapacity)"
    )
    Page<Space> searchMySpaces(
            @Param("hostId") String hostId,
            @Param("namePattern") String namePattern,
            @Param("category") SpaceCategory category,
            @Param("region") Region region,
            @Param("usageUnit") UsageUnit usageUnit,
            @Param("minCapacity") Integer minCapacity,
            @Param("maxCapacity") Integer maxCapacity,
            Pageable pageable
    );

    @Query(
            "SELECT s.category AS category, COUNT(s) AS count FROM Space s "
                    + "WHERE s.deletedAt IS NULL AND s.isActive = true AND s.adminStatus = :adminStatus "
                    + "GROUP BY s.category"
    )
    List<SpaceCategoryCountProjection> countActiveSpacesByCategory(
            @Param("adminStatus") ApprovalStatus adminStatus
    );

    @Query(
            value = "SELECT COALESCE(a.region, 'UNKNOWN') AS region, COUNT(*) AS count "
                    + "FROM spaces.spaces s "
                    + "JOIN spaces.addresses a ON s.address_id = a.id "
                    + "WHERE s.deleted_at IS NULL AND s.is_active = true AND s.admin_status = :adminStatus "
                    + "GROUP BY COALESCE(a.region, 'UNKNOWN')",
            nativeQuery = true
    )
    List<SpaceRegionCountProjection> countActiveSpacesByRegion(
            @Param("adminStatus") String adminStatus
    );
}
