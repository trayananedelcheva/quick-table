import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import { AuthProvider, useAuth } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import HomePage from './pages/HomePage';
import RestaurantsPage from './pages/RestaurantsPage';
import ReservationPage from './pages/ReservationPage';
import MyReservationsPage from './pages/MyReservationsPage';
import AdminRestaurantPage from './pages/AdminRestaurantPage';
import AdminReservationsPage from './pages/AdminReservationsPage';
import AdminUsersPage from './pages/AdminUsersPage';
import ProfilePage from './pages/ProfilePage';
import SettingsPage from './pages/SettingsPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';

const theme = createTheme({
  palette: {
    primary: { main: '#004d5a' },
    background: {
      default: '#f5f0eb',
      paper: '#ffffff',
    },
  },
  components: {
    MuiFilledInput: {
      styleOverrides: {
        root: {
          '& .MuiIconButton-root': {
            backgroundColor: 'transparent !important',
            '&:hover': {
              backgroundColor: 'transparent !important',
            },
          },
        },
      },
    },
  },
});

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { token } = useAuth();
  return token ? <>{children}</> : <Navigate to="/login" />;
};

const RoleRoute: React.FC<{ children: React.ReactNode; role: string }> = ({ children, role }) => {
  const { token, user } = useAuth();
  if (!token) return <Navigate to="/login" />;
  if (user?.role !== role) return <Navigate to="/" />;
  return <>{children}</>;
};

const AppRoutes: React.FC = () => (
  <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />
    <Route path="/forgot-password" element={<ForgotPasswordPage />} />
    <Route path="/reset-password" element={<ResetPasswordPage />} />
    <Route path="/" element={<RestaurantsPage />} />
    <Route path="/home" element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
    <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
    <Route path="/settings" element={<ProtectedRoute><SettingsPage /></ProtectedRoute>} />
    <Route path="/restaurants" element={<RestaurantsPage />} />
    <Route path="/reserve/:restaurantId" element={<ReservationPage />} />
    <Route path="/my-reservations" element={<RoleRoute role="CLIENT"><MyReservationsPage /></RoleRoute>} />
    <Route path="/admin/restaurant" element={<RoleRoute role="RESTAURANT_ADMIN"><AdminRestaurantPage /></RoleRoute>} />
    <Route path="/admin/reservations" element={<RoleRoute role="RESTAURANT_ADMIN"><AdminReservationsPage /></RoleRoute>} />
    <Route path="/admin/users" element={<RoleRoute role="SYSTEM_ADMIN"><AdminUsersPage /></RoleRoute>} />
    <Route path="*" element={<Navigate to="/" />} />
  </Routes>
);

const App: React.FC = () => (
  <ThemeProvider theme={theme}>
    <CssBaseline />
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  </ThemeProvider>
);

export default App;
