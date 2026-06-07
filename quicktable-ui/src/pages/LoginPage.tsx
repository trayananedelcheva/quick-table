import React, { useState } from 'react';
import {
  Box, Button, Container, TextField, Typography, Paper, Alert, Link,
  InputAdornment, IconButton
} from '@mui/material';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import { login } from '../api/auth';
import { useAuth } from '../context/AuthContext';
import { useNavigate, useLocation } from 'react-router-dom';

const LoginPage: React.FC = () => {
  const { setAuth } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const locationState = location.state as any;
  const returnTo = locationState?.returnTo;
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const auth = await login({ email, password });
      setAuth(auth);
      navigate(returnTo || '/home', { state: locationState });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Невалиден имейл или парола.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="xs">
      <Box sx={{ mt: 8 }}>
        <Paper elevation={3} sx={{ p: 4 }}>
          <Typography variant="h5" sx={{ fontWeight: 700, mb: 3, textAlign: 'center' }}>
            Вход в QuickTable
          </Typography>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Box component="form" onSubmit={handleSubmit}>
            <TextField
              label="Имейл"
              type="email"
              fullWidth
              required
              margin="normal"
              variant="filled"
              value={email}
              onChange={e => setEmail(e.target.value)}
            />
            <TextField
              label="Парола"
              fullWidth
              required
              margin="normal"
              variant="filled"
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={e => setPassword(e.target.value)}
              slotProps={{
                input: {
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton onClick={() => setShowPassword(s => !s)} edge="end" tabIndex={-1}>
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                },
              }}
            />
            <Button type="submit" fullWidth variant="contained" size="large" sx={{ mt: 3 }} disabled={loading}>
              {loading ? 'Влизане...' : 'Вход'}
            </Button>
          </Box>
          <Typography sx={{ textAlign: 'center', mt: 2 }} variant="body2">
            <Link href="/forgot-password" underline="hover">Забравена парола?</Link>
          </Typography>
          <Typography sx={{ textAlign: 'center', mt: 1 }} variant="body2">
            Нямате акаунт?{' '}
            <Link href="/register" underline="hover">Регистрирайте се</Link>
          </Typography>
        </Paper>
      </Box>
    </Container>
  );
};

export default LoginPage;
