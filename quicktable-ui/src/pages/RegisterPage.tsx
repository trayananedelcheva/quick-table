import React, { useState } from 'react';
import {
  Box, Button, Container, TextField, Typography, Paper, Alert, Link
} from '@mui/material';
import { register } from '../api/auth';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

const RegisterPage: React.FC = () => {
  const { setAuth } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    phoneNumber: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const auth = await register(form);
      setAuth(auth);
      navigate('/home');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Грешка при регистрация.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="xs">
      <Box sx={{ mt: 8 }}>
        <Paper elevation={3} sx={{ p: 4 }}>
          <Typography variant="h5" sx={{ fontWeight: 700, mb: 3, textAlign: 'center' }}>
            Регистрация в QuickTable
          </Typography>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Box component="form" onSubmit={handleSubmit}>
            <TextField
              label="Име"
              name="firstName"
              fullWidth
              required
              margin="normal"
              variant="filled"
              value={form.firstName}
              onChange={handleChange}
            />
            <TextField
              label="Фамилия"
              name="lastName"
              fullWidth
              required
              margin="normal"
              variant="filled"
              value={form.lastName}
              onChange={handleChange}
            />
            <TextField
              label="Имейл"
              name="email"
              type="email"
              fullWidth
              required
              margin="normal"
              variant="filled"
              value={form.email}
              onChange={handleChange}
            />
            <TextField
              label="Парола"
              name="password"
              type="password"
              fullWidth
              required
              margin="normal"
              variant="filled"
              value={form.password}
              onChange={handleChange}
            />
            <TextField
              label="Телефон (незадължително)"
              name="phoneNumber"
              fullWidth
              margin="normal"
              variant="filled"
              value={form.phoneNumber}
              onChange={handleChange}
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              sx={{ mt: 3 }}
              disabled={loading}
            >
              {loading ? 'Регистрация...' : 'Регистрация'}
            </Button>
          </Box>
          <Typography sx={{ textAlign: 'center', mt: 2 }} variant="body2">
            Вече имате акаунт?{' '}
            <Link href="/login" underline="hover">Влезте</Link>
          </Typography>
        </Paper>
      </Box>
    </Container>
  );
};

export default RegisterPage;
