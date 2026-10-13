import React, { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '@/context/AuthContext';
import api from '@/api/axios';

export const LoginPage = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const auth = useContext(AuthContext);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await api.post('/auth/login', { username, password });
      const token = response.data.token;
      auth?.login(token);
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Login failed');
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-primary to-secondary">
      <form onSubmit={handleSubmit} className="bg-white/30 backdrop-blur-lg p-8 rounded-xl shadow-xl border border-border w-96 max-w-full mx-4">
        <h2 className="text-2xl mb-4 text-center">Login</h2>
        {error && <p className="text-red-500 mb-2">{error}</p>}
        <div className="mb-4">
          <label className="block mb-1">Username</label>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="w-full border border-border rounded px-3 py-2 bg-white/10 backdrop-blur-sm text-foreground focus:ring-2 focus:ring-primary focus:outline-none transition"
            required
          />
        </div>
        <div className="mb-4">
          <label className="block mb-1">Password</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full border border-border rounded px-3 py-2 bg-white/10 backdrop-blur-sm text-foreground focus:ring-2 focus:ring-primary focus:outline-none transition"
            required
          />
        </div>
        <button type="submit" className="w-full bg-primary text-primary-foreground py-2 rounded transition transform hover:scale-105 hover:bg-primary/80 focus:outline-none focus:ring-2 focus:ring-primary">
          Sign In
        </button>
      </form>
    </div>
  );
};
