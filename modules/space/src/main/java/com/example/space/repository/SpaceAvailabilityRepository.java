package com.example.space.repository;

import com.example.space.entity.SpaceAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SpaceAvailabilityRepository extends JpaRepository<SpaceAvailability, Long> {

    List<SpaceAvailability> findAllBySpaceIdOrderByDayOfWeekAscStartTimeAsc(Long spaceId);

    void deleteAllBySpaceId(Long spaceId);

    /**
     * 요청 시간대([startTime,endTime))가 요일별 반복 가용시간 한 슬롯에 완전히
     * 포함되는지 확인한다. {@link com.example.space.entity.SpaceSchedule}(날짜별
     * 실제 예약 인스턴스)에 아직 해당 날짜의 슬롯이 없어도, 호스트가 열어둔 주간
     * 반복 시간대라면 예약을 요청할 수 있게 하기 위한 보조 체크.
     */
    boolean existsBySpaceIdAndDayOfWeekAndIsOpenTrueAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            Long spaceId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    );
}
