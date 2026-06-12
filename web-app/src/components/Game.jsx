import React, { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { gameService, playerService } from '../services/api';
import { Activity, Send, RefreshCcw, CheckCircle, AlertCircle, Sparkles, Zap } from 'lucide-react';
import AchievementToast from './AchievementToast';

const Game = ({ onStreakUpdate }) => {
  const [signal, setSignal] = useState([]);
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [score, setScore] = useState(0);
  const [userPrediction, setUserPrediction] = useState('');
  const [feedback, setFeedback] = useState(null);
  const [errorMsg, setErrorMsg] = useState(null);
  const [gameMode, setGameMode] = useState('Practice');
  const [timeLeft, setTimeLeft] = useState(30);
  const [isTimerActive, setIsTimerActive] = useState(false);
  const [currentCase, setCurrentCase] = useState(null);
  const [showAtlas, setShowAtlas] = useState(false);
  const [streakMultiplier, setStreakMultiplier] = useState(1.0);
  const [newAchievements, setNewAchievements] = useState([]);
  const [correctTypesFound, setCorrectTypesFound] = useState(new Set());
  const [scoreAnim, setScoreAnim] = useState(false);

  const anomalyTypes = [
    { id: 'Normal', label: '💚 Normal', emoji: '💚',
      description: 'R-peaks at a regular rate of 60-100 BPM with stable ST-segment.',
      atlasInfo: 'Rhythmic spikes evenly spaced. Flat baseline between beats.' },
    { id: 'Tachycardia', label: '💛 Tachycardia', emoji: '💛',
      description: 'R-R interval shortened, heart rate above 100 BPM.',
      atlasInfo: 'Heartbeats crowded together. Narrow space between tall spikes.' },
    { id: 'Bradycardia', label: '💙 Bradycardia', emoji: '💙',
      description: 'R-R interval prolonged, heart rate below 60 BPM.',
      atlasInfo: 'Tall spikes far apart. Long flat periods between heartbeats.' },
    { id: 'MI', label: '❤️‍🔥 MI (ST-Elevation)', emoji: '❤️‍🔥',
      description: 'ST-segment elevation indicates acute coronary blockage.',
      atlasInfo: 'Segment after the spike stays high instead of returning to baseline.' },
    { id: 'Murmur', label: '💜 Murmur', emoji: '💜',
      description: 'Irregular noise during diastolic phase, turbulent blood flow.',
      atlasInfo: 'Fuzzy static between main heartbeats — valves not closing smoothly.' }
  ];

  const clinicalCases = {
    'Normal': ["24yo athlete, routine physical. No symptoms.", "35yo female with occasional palpitations. Currently asymptomatic."],
    'Tachycardia': ["19yo student, racing heart after 4 energy drinks.", "42yo male, sudden chest thumping at rest."],
    'Bradycardia': ["78yo female, extreme fatigue and near-fainting.", "32yo marathon runner, pulse 45 bpm, feels fine."],
    'MI': ["62yo diabetic male, crushing chest pain radiating to jaw.", "55yo smoker, heavy chest pressure for 2 hours."],
    'Murmur': ["8yo child, whooshing sound at routine checkup.", "50yo male, shortness of breath, history of rheumatic fever."]
  };

  useEffect(() => {
    let timer;
    if (isTimerActive && timeLeft > 0) {
      timer = setInterval(() => setTimeLeft(prev => prev - 1), 1000);
    } else if (timeLeft === 0 && isTimerActive) {
      setIsTimerActive(false);
      handleAnalyze(true);
    }
    return () => clearInterval(timer);
  }, [isTimerActive, timeLeft]);

  // Load multiplier on mount
  useEffect(() => {
    playerService.getProfile().then(p => {
      setStreakMultiplier(p.streak?.multiplier || 1.0);
    }).catch(() => {});
  }, []);

  const generateSampleData = (newMode = null) => {
    const activeMode = newMode || gameMode;
    const data = [];
    const type = anomalyTypes[Math.floor(Math.random() * anomalyTypes.length)].id;
    let interval = 200;
    if (type === 'Tachycardia') interval = 110;
    if (type === 'Bradycardia') interval = 380;

    for (let i = 0; i < 1000; i++) {
      let val = Math.sin(i * 0.1) * 0.1;
      const beatPos = i % Math.floor(interval);
      if (beatPos === 0) val += 1.5;
      if (beatPos === 20) val -= 0.5;
      if (beatPos === 30) val += 4.0;
      if (beatPos === 40) val -= 0.8;
      if (type === 'MI' && beatPos > 40 && beatPos < 90) val += 1.2;
      if (beatPos === 90) val += 0.6;
      if (type === 'Murmur' && beatPos > 100 && beatPos < 180) val += (Math.random() - 0.5) * 3.5;
      data.push({ time: i, value: val + (Math.random() - 0.5) * 0.05 });
    }
    setSignal(data);
    setResults(null);
    setUserPrediction('');
    setFeedback(null);
    setErrorMsg(null);

    if (activeMode === 'ClinicalCase') {
      const cases = clinicalCases[type];
      setCurrentCase(cases[Math.floor(Math.random() * cases.length)]);
    } else { setCurrentCase(null); }

    if (activeMode === 'TimeAttack') { setTimeLeft(30); setIsTimerActive(true); }
    else { setIsTimerActive(false); }
  };

  const handleAnalyze = async (isAutoSubmit = false) => {
    if (!userPrediction && !isAutoSubmit) { alert('Pick your prediction first!'); return; }
    setIsTimerActive(false);
    setLoading(true);
    setErrorMsg(null);
    try {
      const rawData = signal.map(s => s.value);
      const data = await gameService.analyze(rawData);
      setResults(data);

      const actualAnomaly = data.anomalies.length > 0
        ? (data.anomalies[0].includes('Tachycardia') ? 'Tachycardia'
          : data.anomalies[0].includes('Bradycardia') ? 'Bradycardia'
          : data.anomalies[0].includes('Infarction') ? 'MI' : 'Murmur')
        : 'Normal';

      const isCorrect = userPrediction === actualAnomaly;
      let points = data.score;
      let bonus = 0;
      if (isCorrect) {
        bonus = 200;
        if (gameMode === 'TimeAttack') bonus += timeLeft * 10;
        setCorrectTypesFound(prev => new Set([...prev, actualAnomaly]));
      } else if (isAutoSubmit) { points = 0; }

      // Apply streak multiplier
      const totalEarned = Math.round((points + bonus) * streakMultiplier);
      setScore(prev => prev + totalEarned);
      setScoreAnim(true);
      setTimeout(() => setScoreAnim(false), 600);

      setFeedback({ isCorrect, actual: actualAnomaly, bonus, totalEarned, wasTimeUp: isAutoSubmit });

      // Record game to backend
      try {
        const gameResult = await playerService.recordGame({
          score: totalEarned,
          wasCorrect: isCorrect,
          anomalyType: actualAnomaly,
          timeRemaining: timeLeft,
          gameMode,
          correctTypesFound: [...correctTypesFound, ...(isCorrect ? [actualAnomaly] : [])],
        });

        if (onStreakUpdate) {
          onStreakUpdate(gameResult.streakCurrent, gameResult.streakMultiplier);
        }
        setStreakMultiplier(gameResult.streakMultiplier);

        if (gameResult.unlockedDetails && gameResult.unlockedDetails.length > 0) {
          setNewAchievements(gameResult.unlockedDetails);
        }
      } catch (err) {
        console.warn('Could not record game to server:', err);
      }
    } catch (err) {
      console.error('Analysis failed', err);
      setErrorMsg('Could not connect to backend. Make sure it is running!');
    } finally { setLoading(false); }
  };

  const modeButtons = [
    { key: 'Practice', label: '🎮 Practice', color: 'var(--primary)' },
    { key: 'TimeAttack', label: '🚨 Code Blue', color: 'var(--error)' },
    { key: 'ClinicalCase', label: '🩺 DiaCore', color: 'var(--accent)' },
  ];

  return (
    <div className="container animate-fade-in" style={{ padding: '2rem 0' }}>
      {/* Achievement Toast */}
      {newAchievements.length > 0 && (
        <AchievementToast achievements={newAchievements} onDismiss={() => setNewAchievements([])} />
      )}

      {/* Mode Selector */}
      <div className="flex-center" style={{ gap: '0.75rem', marginBottom: '2rem', flexWrap: 'wrap' }}>
        {modeButtons.map(m => (
          <button
            key={m.key}
            className={gameMode === m.key ? 'btn-primary' : 'btn-outline'}
            onClick={() => { setGameMode(m.key); generateSampleData(m.key); }}
            style={{ padding: '0.6rem 1.5rem', fontSize: '0.8rem' }}
          >
            {m.label}
          </button>
        ))}
      </div>

      {/* Main Game Card */}
      <div className="glass glass-card" style={{ marginBottom: '2rem', position: 'relative' }}>
        {/* Timer Bar */}
        {gameMode === 'TimeAttack' && isTimerActive && (
          <div style={{ position: 'absolute', top: '-0.75rem', left: '10%', right: '10%', textAlign: 'center', zIndex: 10 }}>
            <p style={{ margin: 0, fontWeight: 800, color: timeLeft < 10 ? 'var(--error)' : 'var(--accent)', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
              ⏱ Patient Stability: {Math.round((timeLeft / 30) * 100)}%
            </p>
            <div className="health-bar-container">
              <div className="health-bar-fill" style={{
                width: `${(timeLeft / 30) * 100}%`,
                background: timeLeft < 10 ? 'var(--error)' : timeLeft < 20 ? 'var(--warning)' : 'var(--accent)',
              }} />
            </div>
          </div>
        )}

        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h1 style={{ margin: 0, fontSize: '2rem' }}>
              {gameMode === 'ClinicalCase' ? '🩺 DiaCore' : '💓 Heartbeat Hunter'}
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              {gameMode === 'TimeAttack' ? '🚨 CODE BLUE! Diagnose before time runs out!'
                : gameMode === 'ClinicalCase' ? 'Review history, analyze the signal'
                : 'Read the ECG and make your prediction'}
            </p>
          </div>
          <div className="glass" style={{ padding: '0.75rem 1.5rem', textAlign: 'center', borderRadius: '1rem' }}>
            <p style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.1em' }}>Session Score</p>
            <p style={{
              fontSize: '2rem', fontWeight: 900, margin: 0,
              background: 'linear-gradient(135deg, var(--warning), var(--secondary))',
              WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
              animation: scoreAnim ? 'scorePopIn 0.4s ease' : 'none',
            }}>{score}</p>
            {streakMultiplier > 1 && (
              <span className="streak-multiplier" style={{ fontSize: '0.65rem' }}>🔥 ×{streakMultiplier.toFixed(1)} streak bonus</span>
            )}
          </div>
        </div>

        {/* Clinical Case Info */}
        {gameMode === 'ClinicalCase' && currentCase && (
          <div className="animate-fade-in" style={{
            background: 'rgba(200, 149, 108, 0.1)', padding: '1.25rem', borderRadius: '1rem',
            marginBottom: '1.5rem', borderLeft: '4px solid var(--accent)',
          }}>
            <h4 style={{ color: 'var(--accent)', marginBottom: '0.4rem', textTransform: 'uppercase', fontSize: '0.7rem', letterSpacing: '0.1em' }}>
              📋 Patient History
            </h4>
            <p style={{ fontSize: '1rem', fontWeight: 600, lineHeight: 1.5 }}>"{currentCase}"</p>
          </div>
        )}

        {/* ECG Chart */}
        <div className="glass" style={{
          height: '300px', padding: '1rem', marginBottom: '2rem',
          background: 'rgba(255, 252, 248, 0.8)', border: '1px solid rgba(123, 45, 59, 0.15)',
        }}>
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={signal}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(123,45,59,0.08)" />
              <XAxis dataKey="time" hide />
              <YAxis stroke="var(--text-muted)" domain={[-2, 6]} fontSize={11} tick={{ fill: 'var(--text-muted)' }} />
              <Tooltip
                contentStyle={{ background: 'rgba(255,252,248,0.95)', border: '1px solid var(--glass-border)', borderRadius: '0.5rem', color: 'var(--text-main)' }}
                itemStyle={{ color: 'var(--secondary)' }}
              />
              <Line type="monotone" dataKey="value" stroke="url(#ecgGradient)" strokeWidth={2.5} dot={false} animationDuration={800} />
              <defs>
                <linearGradient id="ecgGradient" x1="0" y1="0" x2="1" y2="0">
                  <stop offset="0%" stopColor="#7B2D3B" />
                  <stop offset="50%" stopColor="#A0424E" />
                  <stop offset="100%" stopColor="#C8956C" />
                </linearGradient>
              </defs>
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Prediction Buttons */}
        <div style={{ marginBottom: '1.5rem' }}>
          <h3 style={{ marginBottom: '0.75rem', fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
            🔍 Your Diagnosis
          </h3>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.6rem' }}>
            {anomalyTypes.map(type => (
              <button
                key={type.id}
                onClick={() => setUserPrediction(type.id)}
                className={userPrediction === type.id ? 'btn-primary' : 'btn-outline'}
                disabled={!!results || (gameMode === 'TimeAttack' && timeLeft === 0)}
                style={{ padding: '0.5rem 1.1rem', fontSize: '0.82rem' }}
              >
                {type.label}
              </button>
            ))}
          </div>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: '0.75rem', borderTop: '1px solid var(--glass-border)', paddingTop: '1.5rem', flexWrap: 'wrap' }}>
          <button className="btn-primary" onClick={() => handleAnalyze(false)} disabled={loading || results || !userPrediction}>
            {loading ? '⏳ Processing...' : <><Send size={16} /> Submit Diagnosis</>}
          </button>
          <button className="btn-outline" onClick={() => generateSampleData()}>
            <RefreshCcw size={16} /> {gameMode === 'TimeAttack' ? 'Next Round' : 'New Patient'}
          </button>
        </div>

        {errorMsg && (
          <div className="animate-fade-in" style={{
            marginTop: '1rem', color: 'var(--error)', display: 'flex', alignItems: 'center', gap: '0.5rem',
            background: 'rgba(192,57,43,0.1)', padding: '0.75rem', borderRadius: '0.75rem', border: '1px solid rgba(192,57,43,0.3)',
          }}>
            <AlertCircle size={16} /> {errorMsg}
          </div>
        )}
      </div>

      {/* Results Card */}
      {results && (
        <div className="animate-fade-in">
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
            <h2 style={{ margin: 0 }}>📊 Clinical Review</h2>
            {feedback && (
              <div className="glass" style={{
                padding: '0.35rem 1rem', borderRadius: '50px',
                background: feedback.isCorrect ? 'rgba(107,143,94,0.15)' : 'rgba(192,57,43,0.15)',
                border: `1px solid ${feedback.isCorrect ? 'var(--accent)' : 'var(--error)'}`,
              }}>
                <span style={{ color: feedback.isCorrect ? 'var(--accent)' : 'var(--error)', fontWeight: 700, fontSize: '0.85rem' }}>
                  {feedback.wasTimeUp ? '⌛ TIME UP' : feedback.isCorrect ? '✨ CORRECT!' : '✗ INCORRECT'}
                </span>
              </div>
            )}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem' }}>
            <div className="glass glass-card">
              <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', color: 'var(--text-main)' }}>
                <Activity size={18} color="var(--secondary)" /> AI Results
              </h3>
              {results.anomalies.length > 0 ? (
                <ul style={{ listStyle: 'none' }}>
                  {results.anomalies.map((a, i) => (
                    <li key={i} style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', padding: '0.6rem 0', borderBottom: '1px solid var(--glass-border)', fontSize: '0.9rem' }}>
                      <AlertCircle size={14} color="var(--error)" /> {a}
                    </li>
                  ))}
                </ul>
              ) : (
                <div style={{ textAlign: 'center', padding: '1.5rem 0' }}>
                  <CheckCircle size={40} color="var(--accent)" style={{ marginBottom: '0.75rem' }} />
                  <p>Healthy Normal signal confirmed ✨</p>
                </div>
              )}
              {feedback && (
                <div style={{
                  marginTop: '1rem', padding: '1rem', background: 'rgba(123,45,59,0.06)',
                  borderRadius: '0.75rem', borderLeft: '3px solid var(--secondary)',
                }}>
                  <p style={{ fontSize: '0.7rem', fontWeight: 800, color: 'var(--text-muted)', marginBottom: '0.25rem', textTransform: 'uppercase' }}>Explanation</p>
                  <p style={{ fontSize: '0.85rem', fontStyle: 'italic', color: 'var(--text-main)' }}>
                    {anomalyTypes.find(t => t.id === feedback.actual)?.description}
                  </p>
                </div>
              )}
            </div>

            <div className="glass glass-card flex-center" style={{ flexDirection: 'column', textAlign: 'center' }}>
              <p style={{ color: 'var(--text-muted)', marginBottom: '0.5rem', fontSize: '0.8rem', textTransform: 'uppercase' }}>Points Earned</p>
              <h2 style={{
                fontSize: '3.5rem', margin: 0,
                background: 'linear-gradient(135deg, var(--accent), var(--warning))',
                WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
                animation: 'scorePopIn 0.6s ease',
              }}>+{feedback?.totalEarned || results.score}</h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginTop: '0.75rem' }}>
                Base: {results.score} • Bonus: {feedback?.bonus || 0}
                {streakMultiplier > 1 && ` • ×${streakMultiplier.toFixed(1)} streak`}
              </p>
              {feedback?.isCorrect && (
                <div style={{ marginTop: '0.75rem', fontSize: '1.5rem' }}>🎉</div>
              )}
              <button className="btn-secondary" style={{ marginTop: '1.25rem' }} onClick={() => generateSampleData()}>
                <Sparkles size={16} /> Next Patient
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ECG Atlas */}
      <div style={{ marginTop: '3rem', borderTop: '1px solid var(--glass-border)', paddingTop: '1.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <div>
            <h2 style={{ margin: 0 }}>📖 Medical Atlas</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Quick reference for ECG patterns</p>
          </div>
          <button className="btn-outline" onClick={() => setShowAtlas(!showAtlas)} style={{ fontSize: '0.8rem' }}>
            {showAtlas ? 'Hide' : 'Show'} Atlas
          </button>
        </div>
        {showAtlas && (
          <div className="atlas-grid animate-fade-in">
            {anomalyTypes.map(type => (
              <div key={type.id} className="atlas-card">
                <h4>{type.emoji} {type.id}</h4>
                <p>{type.atlasInfo}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Game;
