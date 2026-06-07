import React, { useEffect, useState } from 'react';
import {
  Container, Typography, Box, CircularProgress, Alert,
  Card, CardContent, Chip, Button, TextField, MenuItem,
  Dialog, DialogTitle, DialogContent, DialogActions
} from '@mui/material';import { useAuth } from '../context/AuthContext';
import { getMyRestaurants } from '../api/restaurants';
import type { RestaurantResponse } from '../api/restaurants';
import {
  getRestaurantReservations, completeReservation,
  rejectReservation, markNoShow
} from '../api/reservations';
import type { ReservationResponse } from '../api/reservations';
import NavBar from '../components/NavBar';

const STATUS_COLOR: Record<string, 'default' | 'warning' | 'success' | 'error' | 'info' | 'secondary'> = {
  CONFIRMED: 'success',
  CANCELLED: 'error',
  REJECTED: 'error',
  COMPLETED: 'info',
  NO_SHOW: 'secondary',
};

const STATUS_LABEL: Record<string, string> = {
  CONFIRMED: 'Потвърдена',
  CANCELLED: 'Отказана',
  REJECTED: 'Отхвърлена',
  COMPLETED: 'Завършена',
  NO_SHOW: 'Неявяване',
};

const AdminReservationsPage: React.FC = () => {
  const { token } = useAuth();
  const [restaurants, setRestaurants] = useState<RestaurantResponse[]>([]);
  const [restaurantId, setRestaurantId] = useState<number | null>(null);
  const [reservations, setReservations] = useState<ReservationResponse[]>([]);
  const [filterDate, setFilterDate] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [rejectDialog, setRejectDialog] = useState<{ open: boolean; id: number | null }>({ open: false, id: null });
  const [rejectReason, setRejectReason] = useState('');
  const [actionLoading, setActionLoading] = useState<{ id: number; action: string } | null>(null);

  useEffect(() => {
    getMyRestaurants(token!).then(data => {
      setRestaurants(data);
      if (data.length > 0) setRestaurantId(data[0].id);
      else setLoading(false);
    });
  }, [token]);

  useEffect(() => {
    if (!restaurantId) return;
    setLoading(true);
    getRestaurantReservations(token!, restaurantId, filterDate || undefined, filterStatus || undefined)
      .then(setReservations)
      .catch(() => setError('Грешка при зареждане на резервациите.'))
      .finally(() => setLoading(false));
  }, [restaurantId, filterDate, filterStatus, token]);

  const refresh = () => {
    if (!restaurantId) return;
    getRestaurantReservations(token!, restaurantId, filterDate || undefined, filterStatus || undefined)
      .then(setReservations)
      .catch(() => setError('Грешка при опресняване.'));
  };

  const handleComplete = async (id: number) => {
    setActionLoading({ id, action: 'complete' });
    try {
      await completeReservation(token!, id);
      refresh();
    } catch (err: any) {
      setError(err.response?.data?.message || err.response?.data?.error || 'Грешка при завършване на резервацията.');
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
      setError(err.response?.data?.message || err.response?.data?.error || 'Грешка при маркиране на неявяване.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async () => {
    if (!rejectDialog.id) return;
    await rejectReservation(token!, rejectDialog.id, rejectReason);
    setRejectDialog({ open: false, id: null });
    setRejectReason('');
    refresh();
  };

  return (
    <>
      <NavBar />
      <Container maxWidth="md" sx={{ mt: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>
          Резервации
        </Typography>
        {restaurants.length > 1 && (
          <TextField
            label="Избери ресторант"
            variant="filled"
            fullWidth
            select
            sx={{ mb: 3 }}
            value={restaurantId ?? ''}
            onChange={e => setRestaurantId(Number(e.target.value))}
          >
            {restaurants.map(r => (
              <MenuItem key={r.id} value={r.id}>{r.name} — {r.city}</MenuItem>
            ))}
          </TextField>
        )}
        <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
          <TextField
            label="Филтър по дата"
            type="date"
            variant="filled"
            value={filterDate}
            onChange={e => setFilterDate(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
            sx={{ flex: 1 }}
          />
          <TextField
            label="Филтър по статус"
            variant="filled"
            select
            value={filterStatus}
            onChange={e => setFilterStatus(e.target.value)}
            sx={{ flex: 1 }}
          >
            <MenuItem value="">Всички</MenuItem>
            {Object.entries(STATUS_LABEL).map(([val, label]) => (
              <MenuItem key={val} value={val}>{label}</MenuItem>
            ))}
          </TextField>
        </Box>
        {loading && <CircularProgress />}
        {error && <Alert severity="error">{error}</Alert>}
        {!loading && reservations.length === 0 && (
          <Typography color="text.secondary">Няма резервации за показване.</Typography>
        )}
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {reservations.map(r => (
            <Card key={r.id}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>{r.customerName}</Typography>
                  <Chip
                    label={STATUS_LABEL[r.status] || r.status}
                    color={STATUS_COLOR[r.status] || 'default'}
                    size="small"
                  />
                </Box>
                <Typography variant="body2">{r.reservationDate} в {r.reservationTime}</Typography>
                <Typography variant="body2">Гости: {r.numberOfGuests}</Typography>
                {r.specialRequests && (
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>{r.specialRequests}</Typography>
                )}
                {r.status === 'CONFIRMED' && (
                  <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
                    <Button size="small" variant="contained" color="success"
                      disabled={actionLoading?.id === r.id}
                      onClick={() => handleComplete(r.id)}
                      startIcon={actionLoading?.id === r.id && actionLoading.action === 'complete' ? <CircularProgress size={14} color="inherit" /> : undefined}>
                      Завърши
                    </Button>
                    <Button size="small" variant="outlined" color="error"
                      disabled={actionLoading?.id === r.id}
                      onClick={() => setRejectDialog({ open: true, id: r.id })}>
                      Отхвърли
                    </Button>
                    <Button size="small" variant="outlined" color="secondary"
                      disabled={actionLoading?.id === r.id}
                      onClick={() => handleNoShow(r.id)}
                      startIcon={actionLoading?.id === r.id && actionLoading.action === 'noshow' ? <CircularProgress size={14} color="inherit" /> : undefined}>
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
          <TextField
            label="Причина"
            variant="filled"
            fullWidth
            multiline
            rows={3}
            sx={{ mt: 1 }}
            value={rejectReason}
            onChange={e => setRejectReason(e.target.value)}
          />
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

export default AdminReservationsPage;
