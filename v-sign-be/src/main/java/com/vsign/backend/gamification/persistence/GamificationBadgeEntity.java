package com.vsign.backend.gamification.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "gamification_badges")
public class GamificationBadgeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false, length = 80)
    private String userId;

    @Column(name = "badge_id", nullable = false, length = 100)
    private String badgeId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "earned_at", nullable = false, length = 40)
    private String earnedAt;

    protected GamificationBadgeEntity() {
    }

    public String getBadgeId() {
        return badgeId;
    }

    public String getName() {
        return name;
    }

    public String getEarnedAt() {
        return earnedAt;
    }
}
