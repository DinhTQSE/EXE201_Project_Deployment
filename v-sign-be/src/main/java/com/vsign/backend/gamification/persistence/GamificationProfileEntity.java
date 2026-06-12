package com.vsign.backend.gamification.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "gamification_profiles")
public class GamificationProfileEntity {
    @Id
    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "user_id", nullable = false, length = 80)
    private String userId;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(name = "total_xp", nullable = false)
    private int totalXp;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    protected GamificationProfileEntity() {
    }

    public GamificationProfileEntity(String email, String userId, String fullName) {
        this.email = email;
        this.userId = userId;
        this.fullName = fullName;
        this.totalXp = 0;
        this.currentStreak = 0;
        this.longestStreak = 0;
    }

    public String getEmail() {
        return email;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void addXp(int xpDelta) {
        this.totalXp += xpDelta;
    }

    public LocalDate getLastActivityDate() {
        return lastActivityDate;
    }

    public void applyActivity(LocalDate activityDate) {
        if (activityDate == null) {
            return;
        }
        if (lastActivityDate == null) {
            if (currentStreak <= 0) {
                currentStreak = 1;
            }
            longestStreak = Math.max(longestStreak, currentStreak);
            lastActivityDate = activityDate;
            return;
        }
        if (activityDate.isEqual(lastActivityDate)) {
            return;
        }
        if (activityDate.isEqual(lastActivityDate.plusDays(1))) {
            currentStreak += 1;
        } else if (activityDate.isAfter(lastActivityDate.plusDays(1))) {
            currentStreak = 1;
        } else {
            return;
        }
        longestStreak = Math.max(longestStreak, currentStreak);
        lastActivityDate = activityDate;
    }
}
