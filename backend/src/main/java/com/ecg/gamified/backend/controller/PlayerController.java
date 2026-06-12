package com.ecg.gamified.backend.controller;

import com.ecg.gamified.backend.model.Streak;
import com.ecg.gamified.backend.model.User;
import com.ecg.gamified.backend.repository.UserRepository;
import com.ecg.gamified.backend.service.AchievementService;
import com.ecg.gamified.backend.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/player")
@RequiredArgsConstructor
public class PlayerController {

    private final StreakService streakService;
    private final AchievementService achievementService;
    private final UserRepository userRepository;

    /**
     * GET /api/player/profile
     * Returns the full player profile: streak info, achievements, XP/level, stats.
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Streak streak = streakService.getStreak(user.getId());
        List<Map<String, Object>> achievements = achievementService.getAchievementsForUser(user.getId());

        // Calculate XP and level
        long xp = streak.getTotalScore();
        int level = calculateLevel(xp);
        long xpForCurrentLevel = xpForLevel(level);
        long xpForNextLevel = xpForLevel(level + 1);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("username", user.getUsername());

        // Streak data
        Map<String, Object> streakData = new LinkedHashMap<>();
        streakData.put("current", streak.getCurrentStreak());
        streakData.put("longest", streak.getLongestStreak());
        streakData.put("multiplier", streakService.getMultiplier(user.getId()));
        streakData.put("lastPlayedDate", streak.getLastPlayedDate() != null ? streak.getLastPlayedDate().toString() : null);
        response.put("streak", streakData);

        // Stats
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalGames", streak.getTotalGamesPlayed());
        stats.put("totalCorrect", streak.getTotalCorrect());
        stats.put("totalScore", streak.getTotalScore());
        stats.put("accuracy", streak.getTotalGamesPlayed() > 0
                ? Math.round((double) streak.getTotalCorrect() / streak.getTotalGamesPlayed() * 100) : 0);
        response.put("stats", stats);

        // XP / Level
        Map<String, Object> levelData = new LinkedHashMap<>();
        levelData.put("level", level);
        levelData.put("currentXp", xp);
        levelData.put("xpForCurrentLevel", xpForCurrentLevel);
        levelData.put("xpForNextLevel", xpForNextLevel);
        levelData.put("title", getLevelTitle(level));
        response.put("levelInfo", levelData);

        // Achievements
        response.put("achievements", achievements);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/player/record-game
     * Called after each game round. Updates streak, stats, checks achievements.
     * Body: { score, wasCorrect, anomalyType, timeRemaining, gameMode, correctTypesFound }
     */
    @PostMapping("/record-game")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> recordGame(@RequestBody Map<String, Object> request, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        int score = ((Number) request.get("score")).intValue();
        boolean wasCorrect = Boolean.TRUE.equals(request.get("wasCorrect"));
        String anomalyType = (String) request.get("anomalyType");
        int timeRemaining = request.get("timeRemaining") != null
                ? ((Number) request.get("timeRemaining")).intValue() : 0;
        String gameMode = (String) request.get("gameMode");

        // correctTypesFound is a list of anomaly type strings the user has correctly identified so far
        Set<String> correctTypes = new HashSet<>();
        if (request.get("correctTypesFound") instanceof List) {
            correctTypes.addAll((List<String>) request.get("correctTypesFound"));
        }

        // Record game and update streak
        Streak streak = streakService.recordGameResult(user.getId(), score, wasCorrect);

        // Check achievements
        List<String> newlyUnlocked = achievementService.checkAndAward(
                user.getId(), streak, wasCorrect, anomalyType, timeRemaining, gameMode, correctTypes);

        // Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("streakCurrent", streak.getCurrentStreak());
        response.put("streakMultiplier", streakService.getMultiplier(user.getId()));
        response.put("totalScore", streak.getTotalScore());
        response.put("totalGames", streak.getTotalGamesPlayed());
        response.put("totalCorrect", streak.getTotalCorrect());
        response.put("newlyUnlocked", newlyUnlocked);

        // Include achievement definitions for the newly unlocked ones
        if (!newlyUnlocked.isEmpty()) {
            List<Map<String, String>> unlockedDetails = new ArrayList<>();
            for (AchievementService.AchievementDef def : AchievementService.ALL_ACHIEVEMENTS) {
                if (newlyUnlocked.contains(def.key)) {
                    Map<String, String> detail = new LinkedHashMap<>();
                    detail.put("key", def.key);
                    detail.put("title", def.title);
                    detail.put("emoji", def.emoji);
                    detail.put("description", def.description);
                    unlockedDetails.add(detail);
                }
            }
            response.put("unlockedDetails", unlockedDetails);
        }

        return ResponseEntity.ok(response);
    }

    // ── XP / Level Helpers ──────────────────────────────────────
    private int calculateLevel(long xp) {
        // Each level requires progressively more XP: level N needs N*500 XP total
        int level = 1;
        while (xpForLevel(level + 1) <= xp) {
            level++;
        }
        return level;
    }

    private long xpForLevel(int level) {
        // Quadratic scaling: level 1=0, level 2=500, level 3=1500, level 4=3000, ...
        return (long) level * (level - 1) * 250;
    }

    private String getLevelTitle(int level) {
        if (level >= 20) return "Cardiology Legend";
        if (level >= 15) return "Heart Surgeon";
        if (level >= 10) return "Senior Cardiologist";
        if (level >= 7) return "Cardiologist";
        if (level >= 5) return "Resident Doctor";
        if (level >= 3) return "Medical Intern";
        return "Medical Student";
    }
}
