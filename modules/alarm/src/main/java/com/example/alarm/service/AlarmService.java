package com.example.alarm.service;

import com.example.alarm.api.AlarmInternalApi;
import com.example.alarm.dto.request.AlarmCreateRequest;
import com.example.alarm.dto.response.AlarmResponse;
import com.example.alarm.entity.Alarm;
import com.example.alarm.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmService implements AlarmInternalApi {

    private final AlarmRepository alarmRepository;

    public void create(Long matchingId, AlarmCreateRequest request) {
        createAlarm(request.userId(), matchingId, request.description());
    }

    @Override
    @Transactional
    public void createAlarm(String userId, Long matchingId, String description) {
        alarmRepository.save(new Alarm(userId, matchingId, description));
    }

    public List<AlarmResponse> getAlarms(String userId) {
        return alarmRepository.findAllByUserIdOrderByIdDesc(userId).stream()
                .map(AlarmResponse::from)
                .toList();
    }

    @Transactional
    public void updateRead(String userId, Long alarmId) {
        alarmRepository.findByIdAndUserId(alarmId, userId)
                .ifPresent(Alarm::markAsRead);
    }
}
