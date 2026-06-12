package com.ecg.gamified.backend.repository;

import com.ecg.gamified.backend.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByUserId(Long userId);
    boolean existsByUserIdAndAchievementKey(Long userId, String achievementKey);
}
