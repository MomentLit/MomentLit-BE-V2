package com.example.space.repository;

import com.example.space.entity.SpaceBookedDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SpaceBookedDateRepository extends JpaRepository<SpaceBookedDate, Long> {

    boolean existsBySpaceIdAndDate(Long spaceId, LocalDate date);

    List<SpaceBookedDate> findAllBySpaceIdAndDateGreaterThanEqual(Long spaceId, LocalDate from);
}
