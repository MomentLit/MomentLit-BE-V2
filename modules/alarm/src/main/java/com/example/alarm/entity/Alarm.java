package com.example.alarm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "Matchings_Alarm", schema = "alarms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "matching_id", nullable = false)
    private Long matchingId;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    public Alarm(String userId, Long matchingId, String description) {
        this.userId = userId;
        this.matchingId = matchingId;
        this.description = description;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
