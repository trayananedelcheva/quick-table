import React, { useEffect, useState } from 'react';
import {
  Container, Box, Paper, Typography, TextField,
  Button, Alert, Divider, CircularProgress
} from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { getMyProfile, updateProfile, changePassword } from '../api/profile';
import type { UserProfile } from '../api/profile';
import NavBar from '../components/NavBar';

const ProfilePage: React.FC = () => {
  const { token } = useAuth();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phone, setPhone] = useState('');
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileSuccess, setProfileSuccess] = useState('');
  const [profileError, setProfileError] = useState('');

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passSaving, setPassSaving] = useState(false);
  const [passSuccess, setPassSuccess] = useState('');
  const [passError, setPassError] = useState('');

  useEffect(() => {
    getMyProfile(token!)
      .then(p => {
        setProfile(p);
        setFirstName(p.firstName);
        setLastName(p.lastName);
        setPhone(p.phoneNumber || '');
      })
      .finally(() => setLoading(false));
  }, [token]);

  const handleProfileSave = async () => {
    setProfileSaving(true);
    setProfileError('');
    setProfileSuccess('');
    try {
      const updated = await updateProfile(token!, firstName, lastName, phone);
      setProfile(updated);
      setProfileSuccess('Профилът е обновен успешно.');
    } catch (err: any) {
      setProfileError(err.response?.data?.message || 'Грешка при запис.');
    } finally {
      setProfileSaving(false);
    }
  };

  const handlePasswordChange = async () => {
    setPassError('');
    setPassSuccess('');
    if (newPassword !== confirmPassword) {
      setPassError('Новите пароли не съвпадат.');
      return;
    }
    setPassSaving(true);
    try {
      await changePassword(token!, currentPassword, newPassword);
      setPassSuccess('Паролата е сменена успешно.');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: any) {
      setPassError(err.response?.data?.message || 'Грешка при смяна на паролата.');
    } finally {
      setPassSaving(false);
    }
  };

  if (loading) return <><NavBar /><Box sx={{ mt: 4, textAlign: 'center' }}><CircularProgress /></Box></>;

  return (
    <>
      <NavBar />
      <Container maxWidth="sm" sx={{ mt: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>Профил</Typography>

        <Paper elevation={3} sx={{ p: 4, mb: 3 }}>
          <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>Лична информация</Typography>
          <TextField label="Имейл" variant="filled" fullWidth value={profile?.email || ''} disabled sx={{ mb: 2 }} />
          {profileError && <Alert severity="error" sx={{ mb: 2 }}>{profileError}</Alert>}
          {profileSuccess && <Alert severity="success" sx={{ mb: 2 }}>{profileSuccess}</Alert>}
          <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
            <TextField
              label="Име"
              variant="filled"
              fullWidth
              value={firstName}
              onChange={e => setFirstName(e.target.value)}
              autoComplete="given-name"
            />
            <TextField
              label="Фамилия"
              variant="filled"
              fullWidth
              value={lastName}
              onChange={e => setLastName(e.target.value)}
              autoComplete="family-name"
            />
          </Box>
          <TextField
            label="Телефон"
            variant="filled"
            fullWidth
            value={phone}
            onChange={e => setPhone(e.target.value)}
            autoComplete="tel"
            sx={{ mb: 2 }}
          />
          <Button variant="contained" onClick={handleProfileSave} disabled={profileSaving}>
            {profileSaving ? 'Запис...' : 'Запази'}
          </Button>
        </Paper>

        <Paper elevation={3} sx={{ p: 4 }}>
          <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>Смяна на парола</Typography>
          {passError && <Alert severity="error" sx={{ mb: 2 }}>{passError}</Alert>}
          {passSuccess && <Alert severity="success" sx={{ mb: 2 }}>{passSuccess}</Alert>}
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField label="Текуща парола" type="password" variant="filled" fullWidth autoComplete="current-password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} />
            <Divider />
            <TextField label="Нова парола" type="password" variant="filled" fullWidth autoComplete="new-password" value={newPassword} onChange={e => setNewPassword(e.target.value)} />
            <TextField label="Потвърди нова парола" type="password" variant="filled" fullWidth autoComplete="new-password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} />
            <Button variant="contained" onClick={handlePasswordChange} disabled={passSaving || !currentPassword || !newPassword || !confirmPassword}>
              {passSaving ? 'Смяна...' : 'Смени паролата'}
            </Button>
          </Box>
        </Paper>
      </Container>
    </>
  );
};

export default ProfilePage;
