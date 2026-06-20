import React, { useEffect, useState } from 'react';
import {
  Box, Container, Typography, CircularProgress, Alert,
  Card, CardContent, Chip, Button,
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Rating, CardMedia
} from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { getMyReservations, cancelReservation } from '../api/reservations';
import type { ReservationResponse } from '../api/reservations';
import { addReview, getReviewByReservation } from '../api/reviews';
import type { ReviewResponse } from '../api/reviews';
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
  COMPLETED: 'Завършена',
  REJECTED: 'Отхвърлена',
  NO_SHOW: 'Неявяване',
};

const MyReservationsPage: React.FC = () => {
  const { token } = useAuth();
  const navigate = useNavigate();
  const [reservations, setReservations] = useState<ReservationResponse[]>([]);
  const [reviews, setReviews] = useState<Record<number, ReviewResponse>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [cancelDialog, setCancelDialog] = useState<{ open: boolean; id: number | null }>({ open: false, id: null });
  const [reviewDialog, setReviewDialog] = useState<{ open: boolean; reservation: ReservationResponse | null }>({ open: false, reservation: null });
  const [rating, setRating] = useState<number>(5);
  const [comment, setComment] = useState('');
  const [reviewError, setReviewError] = useState('');
  const [reviewSuccess, setReviewSuccess] = useState('');

  useEffect(() => {
    getMyReservations(token!)
      .then(async data => {
        setReservations(data);
        // Зареди отзивите за завършените резервации
        const completed = data.filter(r => r.status === 'COMPLETED');
        const reviewEntries = await Promise.all(
          completed.map(async r => {
            const review = await getReviewByReservation(r.restaurantId, r.id);
            return review ? [r.id, review] as [number, ReviewResponse] : null;
          })
        );
        const reviewMap: Record<number, ReviewResponse> = {};
        reviewEntries.filter(Boolean).forEach(entry => {
          reviewMap[entry![0]] = entry![1];
        });
        setReviews(reviewMap);
      })
      .catch(() => setError('Грешка при зареждане на резервациите.'))
      .finally(() => setLoading(false));
  }, [token]);

  const handleCancel = async () => {
    if (!cancelDialog.id) return;
    try {
      await cancelReservation(token!, cancelDialog.id);
      setReservations(rs => rs.map(r => r.id === cancelDialog.id ? { ...r, status: 'CANCELLED' } : r));
    } catch (err: any) {
      setError(err.response?.data?.message || 'Грешка при отказване.');
    } finally {
      setCancelDialog({ open: false, id: null });
    }
  };

  const handleReviewSubmit = async () => {
    if (!reviewDialog.reservation) return;
    setReviewError('');
    try {
      const review = await addReview(token!, reviewDialog.reservation.restaurantId, {
        reservationId: reviewDialog.reservation.id,
        rating,
        comment: comment || undefined,
      });
      setReviews(prev => ({ ...prev, [reviewDialog.reservation!.id]: review }));
      setReviewSuccess('Отзивът е изпратен успешно!');
      setReviewDialog({ open: false, reservation: null });
      setRating(5);
      setComment('');
    } catch (err: any) {
      setReviewError(err.response?.data?.message || 'Грешка при изпращане на отзив.');
    }
  };

  const canReview = (reservationDate: string) => {
    const date = new Date(reservationDate);
    const deadline = new Date(date);
    deadline.setDate(deadline.getDate() + 14);
    return new Date() <= deadline;
  };

  return (
    <>
      <NavBar />
      <Container maxWidth="md" sx={{ mt: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>
          Моите резервации
        </Typography>
        {loading && <CircularProgress />}
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {reviewSuccess && <Alert severity="success" sx={{ mb: 2 }}>{reviewSuccess}</Alert>}
        {!loading && reservations.length === 0 && (
          <Typography color="text.secondary">Нямате резервации.</Typography>
        )}
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {reservations.map(r => (
            <Card key={r.id} sx={{ display: 'flex' }}>
              <CardMedia
                component="img"
                sx={{ width: 140, flexShrink: 0, objectFit: 'cover' }}
                image={r.restaurantImageUrl || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=300&q=80'}
                alt={r.restaurantName}
              />
              <CardContent sx={{ flex: 1 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="h6" sx={{ fontWeight: 700, cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }} onClick={() => navigate(`/reserve/${r.restaurantId}`)}>
                    {r.restaurantName}
                  </Typography>
                  <Chip label={STATUS_LABEL[r.status] || r.status} color={STATUS_COLOR[r.status] || 'default'} size="small" />
                </Box>
                <Typography variant="body2" sx={{ mt: 1 }}>{r.reservationDate} в {r.reservationTime}</Typography>
                <Typography variant="body2">Гости: {r.numberOfGuests}</Typography>
                {r.specialRequests && (
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>{r.specialRequests}</Typography>
                )}
                <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
                  {r.status === 'CONFIRMED' && (
                    <Button size="small" variant="contained" color="error" onClick={() => setCancelDialog({ open: true, id: r.id })}>
                      Откажи
                    </Button>
                  )}
                  {r.status === 'COMPLETED' && canReview(r.reservationDate) && (
                    reviews[r.id] ? (
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
                        <Rating value={reviews[r.id].rating} readOnly size="small" />
                        {reviews[r.id].comment && (
                          <Typography variant="body2" color="text.secondary">
                            "{reviews[r.id].comment}"
                          </Typography>
                        )}
                      </Box>
                    ) : (
                      <Button size="small" variant="contained" color="primary" onClick={() => { setReviewDialog({ open: true, reservation: r }); setReviewError(''); }}>
                        Остави отзив
                      </Button>
                    )
                  )}
                </Box>
              </CardContent>
            </Card>
          ))}
        </Box>
      </Container>

      {/* Диалог за отказ */}
      <Dialog open={cancelDialog.open} onClose={() => setCancelDialog({ open: false, id: null })} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ color: 'text.primary' }}>Отказване на резервация</DialogTitle>
        <DialogContent>
          <Typography>Сигурни ли сте, че искате да откажете тази резервация?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCancelDialog({ open: false, id: null })}>Не</Button>
          <Button variant="contained" color="error" onClick={handleCancel}>Да, откажи</Button>
        </DialogActions>
      </Dialog>

      {/* Диалог за отзив */}
      <Dialog open={reviewDialog.open} onClose={() => setReviewDialog({ open: false, reservation: null })} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ color: 'text.primary' }}>Отзив за {reviewDialog.reservation?.restaurantName}</DialogTitle>
        <DialogContent>
          {reviewError && <Alert severity="error" sx={{ mb: 2 }}>{reviewError}</Alert>}
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <Box>
              <Typography variant="body2" sx={{ mb: 0.5, color: 'text.primary' }}>Оценка</Typography>
              <Rating value={rating} onChange={(_, v) => setRating(v ?? 1)} />
            </Box>
            <TextField
              label="Коментар (незадължително)"
              variant="filled"
              fullWidth
              multiline
              rows={3}
              value={comment}
              onChange={e => setComment(e.target.value)}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReviewDialog({ open: false, reservation: null })}>Отказ</Button>
          <Button variant="contained" onClick={handleReviewSubmit}>Изпрати</Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default MyReservationsPage;
