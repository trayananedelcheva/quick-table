import React, { useEffect, useState } from 'react';
import {
  Container, Box, Paper, Typography, TextField,
  Button, Alert, Divider, CircularProgress, List,
  ListItemButton, ListItemIcon, ListItemText
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import LockIcon from '@mui/icons-material/Lock';
import { useAuth } from '../context/AuthContext';
import { getMyProfile, updateProfile, changePassword } from '../api/profile';
import type { UserProfile } from '../api/profile';
import NavBar from '../components/NavBar';

const SECTIONS = [
  { key: 'profile', label: 'Профил', icon: <PersonIcon /> },
  { key: 'security', label: 'Парола и сигурност', icon: <LockIcon /> },
];

const SettingsPage: React.FC = () => {
  const { token } = useAuth();
  const [section, setSection] = useState('profile');
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
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>Настройки</Typography>
        <Box sx={{ display: 'flex', gap: 3, alignItems: 'flex-start' }}>

          {/* Sidebar */}
          <Paper elevation={1} sx={{ width: 240, flexShrink: 0 }}>
            <List disablePadding>
              {SECTIONS.map((s, i) => (
                <React.Fragment key={s.key}>
                  {i > 0 && <Divider />}
                  <ListItemButton
                    selected={section === s.key}
                    onClick={() => setSection(s.key)}
                    sx={{
                      borderLeft: section === s.key ? '3px solid' : '3px solid transparent',
                      borderColor: section === s.key ? 'primary.main' : 'transparent',
                      py: 1.5,
                    }}
                  >
                    <ListItemIcon sx={{ minWidth: 36 }}>{s.icon}</ListItemIcon>
                    <ListItemText primary={s.label} />
                  </ListItemButton>
                </React.Fragment>
              ))}
            </List>
          </Paper>

          {/* Content */}
          <Paper elevation={3} sx={{ flex: 1, p: 4 }}>
            {section === 'profile' && (
              <>
                <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>Профил</Typography>
                <Divider sx={{ mb: 3 }} />
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  Имейлът не може да се промени. Останалите данни можеш да редактираш свободно.
                </Typography>
                <TextField label="Имейл" variant="filled" fullWidth value={profile?.email || ''} disabled sx={{ mb: 2 }} />
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
                  sx={{ mb: 3 }}
                />
                {profileError && <Alert severity="error" sx={{ mb: 2 }}>{profileError}</Alert>}
                {profileSuccess && <Alert severity="success" sx={{ mb: 2 }}>{profileSuccess}</Alert>}
                <Button variant="contained" onClick={handleProfileSave} disabled={profileSaving}>
                  {profileSaving ? 'Запис...' : 'Запази промените'}
                </Button>
              </>
            )}

            {section === 'security' && (
              <>
                <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>Парола и сигурност</Typography>
                <Divider sx={{ mb: 3 }} />
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  За смяна на паролата въведи текущата си парола и новата парола два пъти.
                </Typography>
                <TextField
                  label="Текуща парола"
                  type="password"
                  variant="filled"
                  fullWidth
                  autoComplete="current-password"
                  value={currentPassword}
                  onChange={e => setCurrentPassword(e.target.value)}
                  sx={{ mb: 2 }}
                />
                <Divider sx={{ mb: 2 }} />
                <TextField
                  label="Нова парола"
                  type="password"
                  variant="filled"
                  fullWidth
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  sx={{ mb: 2 }}
                />
                <TextField
                  label="Потвърди нова парола"
                  type="password"
                  variant="filled"
                  fullWidth
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={e => setConfirmPassword(e.target.value)}
                  sx={{ mb: 3 }}
                />
                {passError && <Alert severity="error" sx={{ mb: 2 }}>{passError}</Alert>}
                {passSuccess && <Alert severity="success" sx={{ mb: 2 }}>{passSuccess}</Alert>}
                <Button
                  variant="contained"
                  onClick={handlePasswordChange}
                  disabled={passSaving || !currentPassword || !newPassword || !confirmPassword}
                >
                  {passSaving ? 'Смяна...' : 'Смени паролата'}
                </Button>
              </>
            )}
          </Paper>
        </Box>
      </Container>
    </>
  );
};

export default SettingsPage;
