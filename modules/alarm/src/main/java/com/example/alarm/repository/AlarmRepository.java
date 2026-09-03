package com.example.alarm.repository;

import com.example.alarm.entity.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    List<Alarm> findAllByUserIdOrderByIdDesc(String userId);

    Optional<Alarm> findByIdAndUserId(Long id, String userId);
}
