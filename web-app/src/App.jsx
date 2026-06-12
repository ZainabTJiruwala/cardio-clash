import React, { useState, useEffect } from 'react';
import Auth from './components/Auth';
import Game from './components/Game';
import PlayerProfile from './components/PlayerProfile';
import StreakBanner from './components/StreakBanner';
import { authService, playerService } from './services/api';
import { LogOut, User, Activity, Gamepad2, Trophy, BarChart3 } from 'lucide-react';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem('token'));
  const [username, setUsername] = useState(localStorage.getItem('username') || '');
  const [activeTab, setActiveTab] = useState('play');
  const [streakData, setStreakData] = useState({ current: 0, multiplier: 1.0 });

  const handleLoginSuccess = () => {
    setIsAuthenticated(true);
    setUsername(localStorage.getItem('username'));
    loadStreakData();
  };

  const handleLogout = () => {
    authService.logout();
    setIsAuthenticated(false);
    setStreakData({ current: 0, multiplier: 1.0 });
  };

  const loadStreakData = async () => {
    try {
      const profile = await playerService.getProfile();
      setStreakData({
        current: profile.streak?.current || 0,
        multiplier: profile.streak?.multiplier || 1.0,
      });
    } catch (err) {
      // Profile may not exist yet for new users
    }
  };

  const handleStreakUpdate = (newStreak, newMultiplier) => {
    setStreakData({ current: newStreak, multiplier: newMultiplier });
  };

  useEffect(() => {
    if (isAuthenticated) loadStreakData();
  }, [isAuthenticated]);

  if (!isAuthenticated) {
    return <Auth onLoginSuccess={handleLoginSuccess} />;
  }

  return (
    <div style={{ flex: 1 }}>
      <nav className="glass" style={{
        borderBottom: '1px solid var(--glass-border)',
        padding: '0.75rem 0',
        position: 'sticky', top: 0, zIndex: 100,
        background: 'rgba(245, 237, 227, 0.92)',
        backdropFilter: 'blur(20px)',
      }}>
        <div className="container" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div className="flex-center" style={{
              width: '40px', height: '40px',
              background: 'linear-gradient(135deg, var(--primary), var(--secondary))',
              borderRadius: '50%', boxShadow: '0 0 15px var(--primary-glow)',
            }}>
              <Activity size={20} color="white" />
            </div>
            <span style={{
              fontWeight: 900, fontSize: '1.2rem', letterSpacing: '-0.04em',
              background: 'linear-gradient(135deg, var(--primary), var(--secondary))',
              WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
            }}>
              CARDIO CLASH
            </span>
          </div>

          <div className="nav-tabs">
            <button className={`nav-tab ${activeTab === 'play' ? 'active' : ''}`} onClick={() => setActiveTab('play')}>
              <Gamepad2 size={14} /> Play
            </button>
            <button className={`nav-tab ${activeTab === 'profile' ? 'active' : ''}`} onClick={() => setActiveTab('profile')}>
              <Trophy size={14} /> Profile
            </button>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <StreakBanner streak={streakData.current} multiplier={streakData.multiplier} />
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
              <User size={16} />
              <span>{username}</span>
            </div>
            <button className="btn-outline" onClick={handleLogout} style={{ padding: '0.4rem 0.9rem', fontSize: '0.75rem' }}>
              <LogOut size={14} /> Logout
            </button>
          </div>
        </div>
      </nav>

      <main>
        {activeTab === 'play' && <Game onStreakUpdate={handleStreakUpdate} />}
        {activeTab === 'profile' && <PlayerProfile />}
      </main>

      <footer style={{ marginTop: 'auto', padding: '2rem 0', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
        <div className="container">
          <p>© 2026 Cardio Clash • Built with 💓</p>
        </div>
      </footer>
    </div>
  );
}

export default App;
