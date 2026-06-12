package com.vsign.backend.gamification.service;

import com.vsign.backend.gamification.dto.BadgeResponse;
import com.vsign.backend.gamification.dto.LeaderboardEntryResponse;
import com.vsign.backend.gamification.dto.LeaderboardPeriod;
import com.vsign.backend.gamification.dto.LeaderboardResponse;
import com.vsign.backend.gamification.dto.UserProgressSummaryResponse;
import com.vsign.backend.gamification.dto.XpAwardRequest;
import com.vsign.backend.gamification.dto.XpAwardResponse;
import com.vsign.backend.gamification.persistence.GamificationBadgeEntity;
import com.vsign.backend.gamification.persistence.GamificationBadgeRepository;
import com.vsign.backend.gamification.persistence.GamificationProfileEntity;
import com.vsign.backend.gamification.persistence.GamificationProfileRepository;
import com.vsign.backend.gamification.persistence.GamificationXpAwardEntity;
import com.vsign.backend.gamification.persistence.GamificationXpAwardRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GamificationService {
    private static final ZoneId APP_ACTIVITY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String LESSON_COMPLETION_SOURCE = "LESSON_COMPLETION";

    private final GamificationProfileRepository profileRepository;
    private final GamificationBadgeRepository badgeRepository;
    private final GamificationXpAwardRepository xpAwardRepository;

    public GamificationService(
            GamificationProfileRepository profileRepository,
            GamificationBadgeRepository badgeRepository,
            GamificationXpAwardRepository xpAwardRepository
    ) {
        this.profileRepository = profileRepository;
        this.badgeRepository = badgeRepository;
        this.xpAwardRepository = xpAwardRepository;
    }

    @Transactional
    public UserProgressSummaryResponse summary(String email) {
        GamificationProfileEntity profile = profileFor(email);
        List<BadgeResponse> badges = badgeRepository.findByUserIdOrderByIdAsc(profile.getUserId()).stream()
                .map(this::toBadgeResponse)
                .toList();
        return new UserProgressSummaryResponse(
                profile.getUserId(),
                profile.getTotalXp(),
                profile.getCurrentStreak(),
                profile.getLongestStreak(),
                badges
        );
    }

    @Transactional
    public LeaderboardResponse leaderboard(String email, LeaderboardPeriod period, int page, int size) {
        GamificationProfileEntity current = profileFor(email);
        List<LeaderboardEntryResponse> ranked = new ArrayList<>();
        List<GamificationProfileEntity> profiles = profileRepository.findAllByOrderByTotalXpDesc();
        for (int i = 0; i < profiles.size(); i++) {
            GamificationProfileEntity profile = profiles.get(i);
            ranked.add(new LeaderboardEntryResponse(
                    i + 1,
                    profile.getUserId(),
                    profile.getFullName(),
                    profile.getAvatarUrl(),
                    profile.getTotalXp()
            ));
        }
        int from = Math.min(page * size, ranked.size());
        int to = Math.min(from + size, ranked.size());
        LeaderboardEntryResponse currentRow = ranked.stream()
                .filter(row -> row.userId().equals(current.getUserId()))
                .findFirst()
                .orElse(new LeaderboardEntryResponse(0, current.getUserId(), current.getFullName(), current.getAvatarUrl(), current.getTotalXp()));
        return new LeaderboardResponse(period, page, size, ranked.subList(from, to), currentRow);
    }

    @Transactional
    public XpAwardResponse awardXp(String email, XpAwardRequest request) {
        GamificationProfileEntity profile = profileFor(email);
        if (xpAwardRepository.existsByUserIdAndEventId(profile.getUserId(), request.eventId())) {
            return new XpAwardResponse(
                    profile.getUserId(),
                    request.eventId(),
                    profile.getTotalXp(),
                    0,
                    profile.getCurrentStreak(),
                    profile.getLongestStreak(),
                    true
            );
        }
        LocalDate activityDate = request.activityDate() == null ? LocalDate.now(APP_ACTIVITY_ZONE) : request.activityDate();
        xpAwardRepository.save(new GamificationXpAwardEntity(
                profile.getUserId(),
                request.eventId(),
                request.source(),
                request.xpDelta(),
                activityDate
        ));
        profile.addXp(request.xpDelta());
        profile.applyActivity(activityDate);
        profileRepository.save(profile);
        return new XpAwardResponse(
                profile.getUserId(),
                request.eventId(),
                profile.getTotalXp(),
                request.xpDelta(),
                profile.getCurrentStreak(),
                profile.getLongestStreak(),
                false
        );
    }

    @Transactional
    public XpAwardResponse awardLessonCompletion(String email, String lessonId, int xpDelta) {
        return awardXp(email, new XpAwardRequest(
                "lesson-complete:" + lessonId,
                LESSON_COMPLETION_SOURCE,
                Math.max(1, xpDelta),
                LocalDate.now(APP_ACTIVITY_ZONE)
        ));
    }

    private GamificationProfileEntity profileFor(String email) {
        return profileRepository.findById(email)
                .orElseGet(() -> profileRepository.save(new GamificationProfileEntity(
                        email,
                        "user-" + Integer.toUnsignedString(email.hashCode()),
                        email
                )));
    }

    private BadgeResponse toBadgeResponse(GamificationBadgeEntity badge) {
        return new BadgeResponse(badge.getBadgeId(), badge.getName(), badge.getEarnedAt());
    }
}
