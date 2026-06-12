package com.ecg.gamified.backend.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory leaderboard (replaces Redis-based implementation).
 * Data lives in memory for the lifetime of the application.
 */
@Service
public class LeaderboardService {

    private final Map<String, Double> scores = new ConcurrentHashMap<>();

    public void submitScore(String username, double score) {
        scores.put(username, score);
    }

    public Set<String> getTopPlayers(int topN) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Double getPlayerScore(String username) {
        return scores.get(username);
    }

    public Long getPlayerRank(String username) {
        if (!scores.containsKey(username)) return null;

        double playerScore = scores.get(username);
        long rank = scores.values().stream()
                .filter(s -> s > playerScore)
                .count();
        return rank + 1;
    }
}
