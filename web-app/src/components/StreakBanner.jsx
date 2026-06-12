import React from 'react';

const StreakBanner = ({ streak, multiplier }) => {
  if (!streak || streak <= 0) return null;

  return (
    <div className="streak-container">
      <span className="streak-fire">🔥</span>
      <span className="streak-count">{streak}</span>
      <span className="streak-label">day streak</span>
      {multiplier > 1 && (
        <span className="streak-multiplier">×{multiplier.toFixed(1)}</span>
      )}
    </div>
  );
};

export default StreakBanner;
