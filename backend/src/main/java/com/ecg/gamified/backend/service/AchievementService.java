package com.ecg.gamified.backend.service;

import com.ecg.gamified.backend.model.Achievement;
import com.ecg.gamified.backend.model.Streak;
import com.ecg.gamified.backend.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;

    // ── Achievement Definitions ──────────────────────────────────────
    public static class AchievementDef {
        public final String key;
        public final String title;
        public final String emoji;
        public final String description;

        public AchievementDef(String key, String title, String emoji, String description) {
            this.key = key;
            this.title = title;
            this.emoji = emoji;
            this.description = description;
        }
    }

    public static final List<AchievementDef> ALL_ACHIEVEMENTS = List.of(
        new AchievementDef("FIRST_DIAGNOSIS", "First Heartbeat", "💓", "Complete your first ECG analysis"),
        new AchievementDef("PERFECT_DIAGNOSIS", "Eagle Eye", "🦅", "Make a correct prediction"),
        new AchievementDef("STREAK_3", "Getting Warmed Up", "🔥", "Achieve a 3-day streak"),
        new AchievementDef("STREAK_7", "On Fire!", "⚡", "Achieve a 7-day streak"),
        new AchievementDef("STREAK_30", "Unstoppable", "🏆", "Achieve a 30-day streak"),
        new AchievementDef("SPEED_DEMON", "Speed Demon", "💨", "Solve Code Blue with 20+ seconds remaining"),
        new AchievementDef("SCORE_1000", "Thousand Club", "🎯", "Reach 1,000 cumulative score"),
        new AchievementDef("SCORE_5000", "Heart Specialist", "❤️‍🔥", "Reach 5,000 cumulative score"),
        new AchievementDef("ALL_TYPES_FOUND", "Full Spectrum", "🌈", "Correctly identify all 5 anomaly types"),
        new AchievementDef("TEN_CORRECT", "Seasoned Doctor", "🩺", "Get 10 correct predictions")
    );

    /**
     * Check and award achievements based on the current game state.
     * Returns a list of NEWLY unlocked achievement keys.
     */
    public List<String> checkAndAward(Long userId, Streak streak, boolean wasCorrect,
                                       String anomalyType, int timeRemaining, String gameMode,
                                       Set<String> correctTypes) {
        List<String> newlyUnlocked = new ArrayList<>();

        // FIRST_DIAGNOSIS: at least 1 game played
        if (streak.getTotalGamesPlayed() >= 1) {
            if (tryAward(userId, "FIRST_DIAGNOSIS")) newlyUnlocked.add("FIRST_DIAGNOSIS");
        }

        // PERFECT_DIAGNOSIS: got one right
        if (wasCorrect) {
            if (tryAward(userId, "PERFECT_DIAGNOSIS")) newlyUnlocked.add("PERFECT_DIAGNOSIS");
        }

        // Streak milestones
        if (streak.getCurrentStreak() >= 3) {
            if (tryAward(userId, "STREAK_3")) newlyUnlocked.add("STREAK_3");
        }
        if (streak.getCurrentStreak() >= 7) {
            if (tryAward(userId, "STREAK_7")) newlyUnlocked.add("STREAK_7");
        }
        if (streak.getCurrentStreak() >= 30) {
            if (tryAward(userId, "STREAK_30")) newlyUnlocked.add("STREAK_30");
        }

        // SPEED_DEMON: Code Blue (TimeAttack) with 20+ seconds left
        if (wasCorrect && "TimeAttack".equals(gameMode) && timeRemaining >= 20) {
            if (tryAward(userId, "SPEED_DEMON")) newlyUnlocked.add("SPEED_DEMON");
        }

        // Score milestones
        if (streak.getTotalScore() >= 1000) {
            if (tryAward(userId, "SCORE_1000")) newlyUnlocked.add("SCORE_1000");
        }
        if (streak.getTotalScore() >= 5000) {
            if (tryAward(userId, "SCORE_5000")) newlyUnlocked.add("SCORE_5000");
        }

        // ALL_TYPES_FOUND: correctly identified all 5 types at some point
        if (correctTypes != null && correctTypes.size() >= 5) {
            if (tryAward(userId, "ALL_TYPES_FOUND")) newlyUnlocked.add("ALL_TYPES_FOUND");
        }

        // TEN_CORRECT
        if (streak.getTotalCorrect() >= 10) {
            if (tryAward(userId, "TEN_CORRECT")) newlyUnlocked.add("TEN_CORRECT");
        }

        return newlyUnlocked;
    }

    /**
     * Try to award an achievement. Returns true if newly awarded, false if already exists.
     */
    private boolean tryAward(Long userId, String key) {
        if (achievementRepository.existsByUserIdAndAchievementKey(userId, key)) {
            return false;
        }
        Achievement achievement = Achievement.builder()
                .userId(userId)
                .achievementKey(key)
                .build();
        achievementRepository.save(achievement);
        return true;
    }

    /**
     * Get all achievements for a user, both unlocked and locked.
     */
    public List<Map<String, Object>> getAchievementsForUser(Long userId) {
        List<Achievement> unlocked = achievementRepository.findByUserId(userId);
        Set<String> unlockedKeys = unlocked.stream()
                .map(Achievement::getAchievementKey)
                .collect(Collectors.toSet());

        Map<String, Achievement> unlockedMap = unlocked.stream()
                .collect(Collectors.toMap(Achievement::getAchievementKey, a -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (AchievementDef def : ALL_ACHIEVEMENTS) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("key", def.key);
            entry.put("title", def.title);
            entry.put("emoji", def.emoji);
            entry.put("description", def.description);
            entry.put("unlocked", unlockedKeys.contains(def.key));
            if (unlockedMap.containsKey(def.key)) {
                entry.put("unlockedAt", unlockedMap.get(def.key).getUnlockedAt().toString());
            }
            result.add(entry);
        }
        return result;
    }
}
