package com.ecg.gamified.backend.service;

import com.ecg.gamified.backend.model.Streak;
import com.ecg.gamified.backend.repository.StreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final StreakRepository streakRepository;

    /**
     * Records that a user played today. Updates streak logic:
     * - If lastPlayedDate was yesterday -> increment streak
     * - If lastPlayedDate is today -> no-op (already counted)
     * - Otherwise -> reset streak to 1
     */
    public Streak recordActivity(Long userId) {
        Streak streak = streakRepository.findByUserId(userId)
                .orElse(Streak.builder().userId(userId).build());

        LocalDate today = LocalDate.now();
        LocalDate lastPlayed = streak.getLastPlayedDate();

        if (lastPlayed == null) {
            // First time playing
            streak.setCurrentStreak(1);
        } else if (lastPlayed.equals(today)) {
            // Already played today, no streak change
        } else if (lastPlayed.equals(today.minusDays(1))) {
            // Played yesterday — extend streak!
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else {
            // Missed a day — reset
            streak.setCurrentStreak(1);
        }

        // Update longest streak record
        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        streak.setLastPlayedDate(today);
        return streakRepository.save(streak);
    }

    /**
     * Record game stats (called after each round).
     */
    public Streak recordGameResult(Long userId, int score, boolean wasCorrect) {
        Streak streak = recordActivity(userId);
        streak.setTotalGamesPlayed(streak.getTotalGamesPlayed() + 1);
        streak.setTotalScore(streak.getTotalScore() + score);
        if (wasCorrect) {
            streak.setTotalCorrect(streak.getTotalCorrect() + 1);
        }
        return streakRepository.save(streak);
    }

    public Streak getStreak(Long userId) {
        return streakRepository.findByUserId(userId)
                .orElse(Streak.builder().userId(userId).build());
    }

    /**
     * Streak multiplier: 1.0 + min(streak, 10) * 0.1
     * Max multiplier is 2.0x at a 10-day streak.
     */
    public double getMultiplier(Long userId) {
        Streak streak = getStreak(userId);
        return 1.0 + Math.min(streak.getCurrentStreak(), 10) * 0.1;
    }
}
