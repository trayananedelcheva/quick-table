import React, { useState } from 'react';
import {
  Container, Box, Paper, Typography, TextField, Button, Alert, Link
} from '@mui/material';
import { resetPassword } from '../api/auth';
import { useNavigate, useSearchParams } from 'react-router-dom';

const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      setError('Паролите не съвпадат.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await resetPassword(token, newPassword);
      setSuccess('Паролата е сменена успешно.');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Невалиден или изтекъл линк.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="xs">
      <Box sx={{ mt: 8 }}>
        <Paper elevation={3} sx={{ p: 4 }}>
          <Typography variant="h5" sx={{ fontWeight: 700, mb: 3, textAlign: 'center' }}>
            Нова парола
          </Typography>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          {success ? (
            <Alert severity="success">{success}</Alert>
          ) : (
            <Box component="form" onSubmit={handleSubmit}>
              <TextField
                label="Нова парола"
                type="password"
                variant="filled"
                fullWidth
                required
                margin="normal"
                value={newPassword}
                onChange={e => setNewPassword(e.target.value)}
              />
              <TextField
                label="Потвърди парола"
                type="password"
                variant="filled"
                fullWidth
                required
                margin="normal"
                value={confirmPassword}
                onChange={e => setConfirmPassword(e.target.value)}
              />
              <Button type="submit" variant="contained" fullWidth size="large" sx={{ mt: 2 }} disabled={loading || !token}>
                {loading ? 'Смяна...' : 'Смени паролата'}
              </Button>
            </Box>
          )}
          {!token && <Alert severity="warning" sx={{ mt: 2 }}>Невалиден линк за смяна на парола.</Alert>}
          <Typography sx={{ textAlign: 'center', mt: 2 }} variant="body2">
            <Link component="button" underline="hover" onClick={() => navigate('/login')}>
              Обратно към вход
            </Link>
          </Typography>
        </Paper>
      </Box>
    </Container>
  );
};

export default ResetPasswordPage;
