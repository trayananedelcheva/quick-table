import React, { useState } from 'react';
import {
  Container, Box, Paper, Typography, TextField, Button, Alert, Link
} from '@mui/material';
import { forgotPassword } from '../api/auth';
import { useNavigate } from 'react-router-dom';

const ForgotPasswordPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await forgotPassword(email);
      setSuccess('Ако имейлът съществува в системата, ще получите линк за смяна на паролата.');
    } catch {
      setError('Възникна грешка. Опитайте отново.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="xs">
      <Box sx={{ mt: 8 }}>
        <Paper elevation={3} sx={{ p: 4 }}>
          <Typography variant="h5" sx={{ fontWeight: 700, mb: 3, textAlign: 'center' }}>
            Забравена парола
          </Typography>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          {success ? (
            <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>
          ) : (
            <Box component="form" onSubmit={handleSubmit}>
              <TextField
                label="Имейл"
                type="email"
                variant="filled"
                fullWidth
                required
                margin="normal"
                value={email}
                onChange={e => setEmail(e.target.value)}
              />
              <Button type="submit" variant="contained" fullWidth size="large" sx={{ mt: 2 }} disabled={loading}>
                {loading ? 'Изпращане...' : 'Изпрати линк'}
              </Button>
            </Box>
          )}
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

export default ForgotPasswordPage;
