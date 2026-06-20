import React, { useState } from 'react';
import {
  AppBar, Toolbar, Typography, Button, Box, Avatar,
  Menu, MenuItem, Divider, ListItemIcon
} from '@mui/material';
import SettingsIcon from '@mui/icons-material/Settings';
import LogoutIcon from '@mui/icons-material/Logout';
import BookmarksIcon from '@mui/icons-material/Bookmarks';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import QuickTableLogo from './QuickTableLogo';

const NavBar: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleLogout = () => {
    setAnchorEl(null);
    logout();
    navigate('/');
  };

  const navBtn = (label: string, path: string) => {
    const active = location.pathname === path;
    return (
      <Button
        key={path}
        onClick={() => navigate(path)}
        sx={{
          color: active ? 'primary.main' : 'white',
          bgcolor: active ? 'white' : 'transparent',
          borderRadius: 2,
          px: 2,
          fontWeight: active ? 700 : 500,
          textTransform: 'none',
          fontSize: 15,
          '&:hover': {
            bgcolor: active ? 'white' : 'rgba(255,255,255,0.15)',
          },
        }}
      >
        {label}
      </Button>
    );
  };

  return (
    <AppBar position="static" elevation={2}>
      <Toolbar sx={{ gap: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexGrow: 1, cursor: 'pointer' }} onClick={() => navigate('/')}>
          <QuickTableLogo size={42} />
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            QuickTable
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', gap: 0.5, alignItems: 'center' }}>
          {!user && (
            <>
              {navBtn('Ресторанти', '/')}
              <Button
                variant="contained"
                onClick={() => navigate('/login')}
                sx={{
                  bgcolor: 'white',
                  color: 'primary.main',
                  fontWeight: 700,
                  textTransform: 'none',
                  borderRadius: 2,
                  '&:hover': { bgcolor: '#f0f0f0' },
                }}
              >
                Вход
              </Button>
            </>
          )}

          {user?.role === 'CLIENT' && (
            <></>
          )}
          {user?.role === 'RESTAURANT_ADMIN' && (
            <>
              {navBtn('Моят ресторант', '/admin/restaurant')}
              {navBtn('Резервации', '/admin/reservations')}
            </>
          )}
          {user?.role === 'SYSTEM_ADMIN' && (
            <>
              {navBtn('Потребители', '/admin/users')}
              {navBtn('Резервации', '/admin/reservations-all')}
            </>
          )}

          {user && (
            <>
              <Box
                onClick={e => setAnchorEl(e.currentTarget)}
                sx={{ display: 'flex', alignItems: 'center', gap: 1, ml: 1, cursor: 'pointer', borderRadius: 2, px: 1.5, py: 0.5, '&:hover': { bgcolor: 'rgba(255,255,255,0.15)' } }}
              >
                <Avatar sx={{ width: 32, height: 32, bgcolor: 'white', color: 'primary.main', fontWeight: 700, fontSize: 14 }}>
                  {user.firstName?.charAt(0)}
                </Avatar>
                <Typography variant="body2" sx={{ color: 'white', fontWeight: 500 }}>
                  {user.firstName}
                </Typography>
              </Box>

              <Menu
                anchorEl={anchorEl}
                open={Boolean(anchorEl)}
                onClose={() => setAnchorEl(null)}
                transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
                slotProps={{ paper: { sx: { mt: 1, minWidth: 180 } } }}
              >
                <MenuItem disabled sx={{ opacity: 1 }}>
                  <Typography variant="body2" color="text.secondary">
                    {user.firstName} {user.lastName}
                  </Typography>
                </MenuItem>
                <Divider />
                <MenuItem onClick={() => { setAnchorEl(null); navigate('/settings'); }}>
                  <ListItemIcon><SettingsIcon fontSize="small" /></ListItemIcon>
                  Настройки
                </MenuItem>
                {user?.role === 'CLIENT' && (
                  <MenuItem onClick={() => { setAnchorEl(null); navigate('/my-reservations'); }}>
                    <ListItemIcon><BookmarksIcon fontSize="small" /></ListItemIcon>
                    Моите резервации
                  </MenuItem>
                )}
                <Divider />
                <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
                  <ListItemIcon><LogoutIcon fontSize="small" color="error" /></ListItemIcon>
                  Изход
                </MenuItem>
              </Menu>
            </>
          )}
        </Box>
      </Toolbar>
    </AppBar>
  );
};

export default NavBar;
