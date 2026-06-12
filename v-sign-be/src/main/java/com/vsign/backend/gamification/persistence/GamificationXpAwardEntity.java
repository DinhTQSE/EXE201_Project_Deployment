package com.vsign.backend.gamification.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "gamification_xp_awards")
public class GamificationXpAwardEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false, length = 80)
    private String userId;

    @Column(name = "event_id", nullable = false, length = 120)
    private String eventId;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(name = "xp_delta", nullable = false)
    private int xpDelta;

    @Column(name = "activity_date")
    private LocalDate activityDate;

    protected GamificationXpAwardEntity() {
    }

    public GamificationXpAwardEntity(String userId, String eventId, String source, int xpDelta, LocalDate activityDate) {
        this.userId = userId;
        this.eventId = eventId;
        this.source = source;
        this.xpDelta = xpDelta;
        this.activityDate = activityDate;
    }
}
