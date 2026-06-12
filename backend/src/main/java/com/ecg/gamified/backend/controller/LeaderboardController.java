package com.ecg.gamified.backend.controller;

import com.ecg.gamified.backend.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @PostMapping("/submit")
    public ResponseEntity<?> submitScore(@RequestBody Map<String, Object> request, Authentication auth) {
        String username = auth.getName();
        double score = ((Number) request.get("score")).doubleValue();

        // Get current score and add to it (cumulative)
        Double currentScore = leaderboardService.getPlayerScore(username);
        double newScore = (currentScore != null ? currentScore : 0) + score;
        leaderboardService.submitScore(username, newScore);

        Map<String, Object> response = new HashMap<>();
        response.put("totalScore", newScore);
        response.put("rank", leaderboardService.getPlayerRank(username));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top")
    public ResponseEntity<?> getTopPlayers(@RequestParam(defaultValue = "10") int count) {
        Set<String> topPlayers = leaderboardService.getTopPlayers(count);
        List<Map<String, Object>> result = new ArrayList<>();
        AtomicInteger rank = new AtomicInteger(1);

        if (topPlayers != null) {
            topPlayers.forEach(player -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("rank", rank.getAndIncrement());
                entry.put("username", player);
                entry.put("score", leaderboardService.getPlayerScore(player));
                result.add(entry);
            });
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyRank(Authentication auth) {
        String username = auth.getName();
        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("score", leaderboardService.getPlayerScore(username));
        response.put("rank", leaderboardService.getPlayerRank(username));
        return ResponseEntity.ok(response);
    }
}
