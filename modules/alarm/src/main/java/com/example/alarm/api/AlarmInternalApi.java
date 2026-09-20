package com.example.alarm.api;

/** Other modules (e.g. matching) inject this to raise an alarm without going through the authenticated HTTP endpoint. */
public interface AlarmInternalApi {

    void createAlarm(String userId, Long matchingId, String description);
}
