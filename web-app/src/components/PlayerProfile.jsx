import React, { useState, useEffect } from 'react';
import { playerService } from '../services/api';
import { Trophy, Target, Zap, BarChart3, Star, Crown } from 'lucide-react';

const PlayerProfile = () => {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => { loadProfile(); }, []);

  const loadProfile = async () => {
    try {
      const data = await playerService.getProfile();
      setProfile(data);
    } catch (err) {
      console.error('Failed to load profile', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="container flex-center" style={{ padding: '4rem 0' }}>
        <div className="glass glass-card" style={{ textAlign: 'center', padding: '3rem' }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem', animation: 'float 2s ease-in-out infinite' }}>💓</div>
          <p style={{ color: 'var(--text-muted)' }}>Loading your profile...</p>
        </div>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="container" style={{ padding: '4rem 0', textAlign: 'center' }}>
        <div className="glass glass-card">
          <p style={{ color: 'var(--text-muted)' }}>Play some games to build your profile! 🎮</p>
        </div>
      </div>
    );
  }

  const { streak, stats, levelInfo, achievements } = profile;
  const xpProgress = levelInfo.xpForNextLevel > levelInfo.xpForCurrentLevel
    ? ((levelInfo.currentXp - levelInfo.xpForCurrentLevel) / (levelInfo.xpForNextLevel - levelInfo.xpForCurrentLevel)) * 100
    : 100;

  return (
    <div className="container animate-fade-in" style={{ padding: '2rem 0' }}>
      {/* Hero Card */}
      <div className="glass glass-card" style={{ marginBottom: '2rem', textAlign: 'center', position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(135deg, rgba(123,45,59,0.08), rgba(160,66,78,0.04))', pointerEvents: 'none' }} />
        <div style={{ position: 'relative' }}>
          <div style={{ fontSize: '4rem', marginBottom: '0.5rem' }}>
            {levelInfo.level >= 10 ? '👑' : levelInfo.level >= 5 ? '⭐' : '🩺'}
          </div>
          <h1 style={{ marginBottom: '0.25rem' }}>{profile.username}</h1>
          <p style={{ color: 'var(--secondary)', fontWeight: 700, fontSize: '0.9rem', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
            {levelInfo.title}
          </p>

          {/* XP Bar */}
          <div style={{ maxWidth: '400px', margin: '1.5rem auto 0' }}>
            <div className="xp-bar-container">
              <span className="xp-level">Lv.{levelInfo.level}</span>
              <div className="xp-bar">
                <div className="xp-bar-fill" style={{ width: `${Math.min(xpProgress, 100)}%` }} />
              </div>
              <span className="xp-level">Lv.{levelInfo.level + 1}</span>
            </div>
            <p style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
              {levelInfo.currentXp} / {levelInfo.xpForNextLevel} XP
            </p>
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <div className="stat-card animate-fade-in" style={{ animationDelay: '0.1s' }}>
          <div style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>🔥</div>
          <div className="stat-value">{streak.current}</div>
          <div className="stat-label">Day Streak</div>
        </div>
        <div className="stat-card animate-fade-in" style={{ animationDelay: '0.2s' }}>
          <div style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>🏅</div>
          <div className="stat-value">{streak.longest}</div>
          <div className="stat-label">Best Streak</div>
        </div>
        <div className="stat-card animate-fade-in" style={{ animationDelay: '0.3s' }}>
          <div style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>🎮</div>
          <div className="stat-value">{stats.totalGames}</div>
          <div className="stat-label">Games Played</div>
        </div>
        <div className="stat-card animate-fade-in" style={{ animationDelay: '0.4s' }}>
          <div style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>🎯</div>
          <div className="stat-value">{stats.accuracy}%</div>
          <div className="stat-label">Accuracy</div>
        </div>
        <div className="stat-card animate-fade-in" style={{ animationDelay: '0.5s' }}>
          <div style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>⚡</div>
          <div className="stat-value">×{streak.multiplier?.toFixed(1)}</div>
          <div className="stat-label">Multiplier</div>
        </div>
      </div>

      {/* Achievements */}
      <div className="glass glass-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
          <Trophy size={22} color="var(--warning)" />
          <h2 style={{ margin: 0 }}>Achievements</h2>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: 'auto' }}>
            {achievements.filter(a => a.unlocked).length} / {achievements.length} unlocked
          </span>
        </div>
        <div className="achievements-grid">
          {achievements.map((ach) => (
            <div key={ach.key} className={`achievement-card ${ach.unlocked ? 'unlocked' : 'locked'}`}>
              <span className="emoji">{ach.emoji}</span>
              <div className="name">{ach.title}</div>
              <div className="desc">{ach.description}</div>
              {ach.unlocked && ach.unlockedAt && (
                <div className="unlocked-date">
                  ✓ {new Date(ach.unlockedAt).toLocaleDateString()}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default PlayerProfile;
