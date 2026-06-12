import React, { useState, useEffect } from 'react';

const AchievementToast = ({ achievements, onDismiss }) => {
  const [current, setCurrent] = useState(0);
  const [hiding, setHiding] = useState(false);

  useEffect(() => {
    if (!achievements || achievements.length === 0) return;
    const timer = setTimeout(() => {
      setHiding(true);
      setTimeout(() => {
        if (current < achievements.length - 1) {
          setCurrent(prev => prev + 1);
          setHiding(false);
        } else {
          onDismiss && onDismiss();
        }
      }, 400);
    }, 3500);
    return () => clearTimeout(timer);
  }, [current, achievements]);

  if (!achievements || achievements.length === 0) return null;
  const ach = achievements[current];

  return (
    <>
      {/* Confetti burst */}
      {!hiding && Array.from({ length: 20 }).map((_, i) => (
        <div
          key={i}
          className="confetti-piece"
          style={{
            left: `${Math.random() * 100}%`,
            backgroundColor: ['#7B2D3B', '#A0424E', '#C8956C', '#D0A889', '#8B3A4A'][i % 5],
            borderRadius: i % 2 === 0 ? '50%' : '2px',
            width: `${6 + Math.random() * 8}px`,
            height: `${6 + Math.random() * 8}px`,
            animationDelay: `${Math.random() * 0.5}s`,
            animationDuration: `${2 + Math.random() * 2}s`,
          }}
        />
      ))}
      <div className={`unlock-notification ${hiding ? 'hiding' : ''}`}>
        <span className="emoji" style={{ animation: 'bounceIn 0.5s ease' }}>{ach.emoji}</span>
        <div>
          <div className="title">🎉 Achievement Unlocked!</div>
          <div className="badge-name">{ach.title}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
            {ach.description}
          </div>
        </div>
      </div>
    </>
  );
};

export default AchievementToast;
