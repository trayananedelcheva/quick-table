import React, { useEffect, useState } from 'react';
import {
  Container, Typography, Box, CircularProgress, Alert,
  Card, CardContent, Chip, TextField, MenuItem, Button,
  Dialog, DialogTitle, DialogContent, DialogActions
} from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { getAllReservations, completeReservation, rejectReservation, markNoShow } from '../api/reservations';
import type { ReservationResponse } from '../api/reservations';
import { getAllRestaurants } from '../api/restaurants';
import type { RestaurantResponse } from '../api/restaurants';
import NavBar from '../components/NavBar';

const STATUS_COLOR: Record<string, 'default' | 'warning' | 'success' | 'error' | 'info' | 'secondary'> = {
  CONFIRMED: 'success',
  CANCELLED: 'error',
  REJECTED: 'error',
  COMPLETED: 'info',
  NO_SHOW: 'warning',
};

const STATUS_LABEL: Record<string, string> = {
  CONFIRMED: 'Потвърдена',
  CANCELLED: 'Отказана',
  REJECTED: 'Отхвърлена',
  COMPLETED: 'Завършена',
  NO_SHOW: 'Неявяване',
};

const AdminAllReservationsPage: React.FC = () => {
  const { token } = useAuth();
  const [reservations, setReservations] = useState<ReservationResponse[]>([]);
  const [restaurants, setRestaurants] = useState<RestaurantResponse[]>([]);
  const [filtered, setFiltered] = useState<ReservationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [restaurantFilter, setRestaurantFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [actionLoading, setActionLoading] = useState<{ id: number; action: string } | null>(null);
  const [rejectDialog, setRejectDialog] = useState<{ open: boolean; id: number | null }>({ open: false, id: null });
  const [rejectReason, setRejectReason] = useState('');

  useEffect(() => {
    Promise.all([
      getAllReservations(token!),
      getAllRestaurants(token!),
    ])
      .then(([res, rest]) => {
        setReservations(res);
        setFiltered(res);
        setRestaurants(rest);
      })
      .catch(() => setError('Грешка при зареждане.'))
      .finally(() => setLoading(false));
  }, [token]);

  useEffect(() => {
    let result = reservations;
    if (restaurantFilter) result = result.filter(r => String(r.restaurantId) === restaurantFilter);
    if (statusFilter) result = result.filter(r => r.status === statusFilter);
    setFiltered(result);
  }, [restaurantFilter, statusFilter, reservations]);

  const refresh = () => {
    getAllReservations(token!).then(data => {
      setReservations(data);
    });
  };

  const handleComplete = async (id: number) => {
    setActionLoading({ id, action: 'complete' });
    try {
      await completeReservation(token!, id);
      refresh();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Грешка при завършване.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleNoShow = async (id: number) => {
    setActionLoading({ id, action: 'noshow' });
    try {
      await markNoShow(token!, id);
      refresh();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Грешка при маркиране.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async () => {
    if (!rejectDialog.id) return;
    try {
      await rejectReservation(token!, rejectDialog.id, rejectReason);
      setRejectDialog({ open: false, id: null });
      setRejectReason('');
      refresh();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Грешка при отхвърляне.');
    }
  };

  return (
    <>
      <NavBar />
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>
          Всички резервации
        </Typography>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

        <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
          <TextField
            label="Филтър по ресторант"
            variant="filled"
            select
            value={restaurantFilter}
            onChange={e => setRestaurantFilter(e.target.value)}
            sx={{ flex: 1, minWidth: 220 }}
          >
            <MenuItem value="">Всички ресторанти</MenuItem>
            {restaurants.map(r => (
              <MenuItem key={r.id} value={String(r.id)}>{r.name} — {r.city}</MenuItem>
            ))}
          </TextField>
          <TextField
            label="Филтър по статус"
            variant="filled"
            select
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
            sx={{ width: 220 }}
          >
            <MenuItem value="">Всички статуси</MenuItem>
            {Object.entries(STATUS_LABEL).map(([val, label]) => (
              <MenuItem key={val} value={val}>{label}</MenuItem>
            ))}
          </TextField>
        </Box>

        {loading && <CircularProgress />}
        {!loading && filtered.length === 0 && (
          <Typography color="text.secondary">Няма резервации.</Typography>
        )}

        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Общо: {filtered.length} резервации
        </Typography>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {filtered.map(r => (
            <Card key={r.id}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Box>
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>{r.restaurantName}</Typography>
                    <Typography variant="body2" color="text.secondary">{r.customerName} · {r.customerEmail}</Typography>
                  </Box>
                  <Chip label={STATUS_LABEL[r.status] || r.status} color={STATUS_COLOR[r.status] || 'default'} size="small" />
                </Box>
                <Typography variant="body2">{r.reservationDate} в {r.reservationTime}</Typography>
                <Typography variant="body2">Гости: {r.numberOfGuests}</Typography>
                {r.specialRequests && (
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>{r.specialRequests}</Typography>
                )}
                {r.status === 'CONFIRMED' && (
                  <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
                    <Button size="small" variant="contained" color="success"
                      disabled={actionLoading?.id === r.id}
                      startIcon={actionLoading?.id === r.id && actionLoading.action === 'complete' ? <CircularProgress size={14} color="inherit" /> : undefined}
                      onClick={() => handleComplete(r.id)}>
                      Завърши
                    </Button>
                    <Button size="small" variant="outlined" color="error"
                      disabled={actionLoading?.id === r.id}
                      onClick={() => setRejectDialog({ open: true, id: r.id })}>
                      Отхвърли
                    </Button>
                    <Button size="small" variant="outlined" color="secondary"
                      disabled={actionLoading?.id === r.id}
                      startIcon={actionLoading?.id === r.id && actionLoading.action === 'noshow' ? <CircularProgress size={14} color="inherit" /> : undefined}
                      onClick={() => handleNoShow(r.id)}>
                      Неявяване
                    </Button>
                  </Box>
                )}
              </CardContent>
            </Card>
          ))}
        </Box>
      </Container>

      <Dialog open={rejectDialog.open} onClose={() => setRejectDialog({ open: false, id: null })} maxWidth="xs" fullWidth>
        <DialogTitle>Отхвърляне на резервация</DialogTitle>
        <DialogContent>
          <TextField label="Причина" variant="filled" fullWidth multiline rows={3} sx={{ mt: 1 }}
            value={rejectReason} onChange={e => setRejectReason(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialog({ open: false, id: null })}>Отказ</Button>
          <Button variant="contained" color="error" onClick={handleReject} disabled={!rejectReason.trim()}>
            Отхвърли
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default AdminAllReservationsPage;
