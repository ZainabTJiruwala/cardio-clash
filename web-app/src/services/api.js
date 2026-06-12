import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor to add JWT token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor to handle authentication errors (401/403)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      // Clear local storage and reload to force login screen
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      window.location.reload();
    }
    return Promise.reject(error);
  }
);

export const authService = {
  login: async (username, password) => {
    const response = await api.post('/auth/login', { username, password });
    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('username', username);
    }
    return response.data;
  },
  register: async (username, email, password) => {
    const response = await api.post('/auth/register', { username, email, password });
    return response.data;
  },
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
  },
};

export const gameService = {
  analyze: async (signalData) => {
    const response = await api.post('/game/analyze', { data: signalData });
    return response.data;
  },
};

export const leaderboardService = {
  getTop: async (count = 10) => {
    const response = await api.get(`/leaderboard/top?count=${count}`);
    return response.data;
  },
  submitScore: async (score) => {
    const response = await api.post('/leaderboard/submit', { score });
    return response.data;
  },
  getMyRank: async () => {
    const response = await api.get('/leaderboard/me');
    return response.data;
  },
};

export const playerService = {
  getProfile: async () => {
    const response = await api.get('/player/profile');
    return response.data;
  },
  recordGame: async (data) => {
    // data: { score, wasCorrect, anomalyType, timeRemaining, gameMode, correctTypesFound }
    const response = await api.post('/player/record-game', data);
    return response.data;
  },
};

export default api;
