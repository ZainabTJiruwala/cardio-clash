import React, { useState } from 'react';
import { authService } from '../services/api';
import { LogIn, UserPlus, HeartPulse } from 'lucide-react';

const Auth = ({ onLoginSuccess }) => {
  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    
    // Fallback to direct DOM values in case browser autofill did not trigger React's onChange
    const usernameVal = e.target.username?.value || formData.username;
    const passwordVal = e.target.password?.value || formData.password;
    const emailVal = e.target.email?.value || formData.email;

    try {
      if (isLogin) {
        await authService.login(usernameVal, passwordVal);
        onLoginSuccess();
      } else {
        await authService.register(usernameVal, emailVal, passwordVal);
        setIsLogin(true);
        setError('Registration successful! Please login.');
      }
    } catch (err) {
      if (err.response && err.response.data) {
        const data = err.response.data;
        if (typeof data === 'string') setError(data);
        else if (data.message) setError(data.message);
        else if (data.error) setError(data.error);
        else setError('An error occurred. Please try again.');
      } else {
        setError('Unable to connect to the server. Please try again.');
      }
    } finally { setLoading(false); }
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  return (
    <div className="container flex-center" style={{ minHeight: '100vh' }}>
      <div className="glass glass-card animate-fade-in" style={{ width: '100%', maxWidth: '420px', position: 'relative', overflow: 'hidden' }}>
        {/* Decorative glow */}
        <div style={{
          position: 'absolute', top: '-50px', right: '-50px', width: '150px', height: '150px',
          background: 'radial-gradient(circle, rgba(160,66,78,0.2), transparent 70%)',
          borderRadius: '50%', pointerEvents: 'none',
        }} />
        <div style={{
          position: 'absolute', bottom: '-30px', left: '-30px', width: '120px', height: '120px',
          background: 'radial-gradient(circle, rgba(123,45,59,0.2), transparent 70%)',
          borderRadius: '50%', pointerEvents: 'none',
        }} />

        <div style={{ position: 'relative' }}>
          <div className="flex-center" style={{ marginBottom: '1.5rem' }}>
            <div className="flex-center animate-float" style={{
              width: '70px', height: '70px', borderRadius: '50%',
              background: 'linear-gradient(135deg, var(--primary), var(--secondary))',
              boxShadow: '0 0 30px var(--primary-glow)',
            }}>
              <HeartPulse size={36} color="white" />
            </div>
          </div>

          <h1 style={{ textAlign: 'center', fontSize: '2.25rem', marginBottom: '0.25rem' }}>Cardio Clash</h1>
          <p style={{ textAlign: 'center', color: 'var(--text-muted)', marginBottom: '2rem', fontSize: '0.9rem' }}>
            {isLogin ? '💓 Welcome back, Doctor!' : '🩺 Join the elite ranks'}
          </p>

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <input 
              id="username"
              type="text" 
              name="username" 
              placeholder="👤 Username" 
              required 
              autoComplete="username"
              value={formData.username} 
              onChange={handleChange} 
            />
            {!isLogin && (
              <input 
                id="email"
                type="email" 
                name="email" 
                placeholder="📧 Email Address" 
                required 
                autoComplete="email"
                value={formData.email} 
                onChange={handleChange} 
              />
            )}
            <input 
              id="password"
              type="password" 
              name="password" 
              placeholder="🔒 Password" 
              required 
              autoComplete={isLogin ? "current-password" : "new-password"}
              value={formData.password} 
              onChange={handleChange} 
            />

            {error && (
              <p style={{
                color: error.includes('successful') ? 'var(--accent)' : 'var(--error)',
                fontSize: '0.85rem', textAlign: 'center',
                background: error.includes('successful') ? 'rgba(107,143,94,0.15)' : 'rgba(192,57,43,0.1)',
                padding: '0.5rem', borderRadius: '0.5rem',
              }}>
                {error}
              </p>
            )}

            <button type="submit" className="btn-primary flex-center" disabled={loading} style={{ width: '100%', justifyContent: 'center', padding: '0.9rem' }}>
              {loading ? '⏳ Processing...' : (isLogin ? <><LogIn size={18} /> Enter the Arena</> : <><UserPlus size={18} /> Create Account</>)}
            </button>
          </form>

          <div style={{ marginTop: '1.25rem', textAlign: 'center' }}>
            <button
              className="btn-outline"
              onClick={() => { setIsLogin(!isLogin); setError(''); }}
              style={{ fontSize: '0.85rem', border: 'none', background: 'transparent', color: 'var(--text-muted)' }}
            >
              {isLogin ? "Don't have an account? Sign Up ✨" : "Already have an account? Login 💓"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Auth;
