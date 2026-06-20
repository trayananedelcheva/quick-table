import React, { useEffect, useState } from 'react';
import {
  Container, Box, CircularProgress, Alert, Typography,
  Paper, Rating, TextField, Button, Dialog,
  DialogTitle, DialogContent, DialogActions
} from '@mui/material';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getMyReservations } from '../api/reservations';
import type { ReservationResponse } from '../api/reservations';
import { addReview, getReviewByReservation } from '../api/reviews';
import NavBar from '../components/NavBar';

const LeaveReviewPage: React.FC = () => {
  const { reservationId } = useParams<{ reservationId: string }>();
  const { token, user } = useAuth();
  const navigate = useNavigate();

  const [reservation, setReservation] = useState<ReservationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [alreadyReviewed, setAlreadyReviewed] = useState(false);
  const [rating, setRating] = useState<number>(5);
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [reviewError, setReviewError] = useState('');

  useEffect(() => {
    if (!token) {
      navigate('/login', { state: { returnTo: `/leave-review/${reservationId}` } });
      return;
    }

    getMyReservations(token!)
      .then(async reservations => {
        const r = reservations.find(r => r.id === Number(reservationId));
        if (!r) {
          setError('Резервацията не е намерена или не принадлежи на вашия акаунт.');
          return;
        }
        if (r.status !== 'COMPLETED') {
          setError('Може да оставите отзив само за приключена резервация.');
          return;
        }
        const deadline = new Date(r.reservationDate);
        deadline.setDate(deadline.getDate() + 14);
        if (new Date() > deadline) {
          setError('Срокът за оставяне на отзив е изтекъл (2 седмици след резервацията).');
          return;
        }
        setReservation(r);

        const existing = await getReviewByReservation(r.restaurantId, r.id);
        if (existing) setAlreadyReviewed(true);
      })
      .catch(() => setError('Грешка при зареждане.'))
      .finally(() => setLoading(false));
  }, [reservationId, token, navigate]);

  const handleSubmit = async () => {
    if (!reservation) return;
    setSubmitting(true);
    setReviewError('');
    try {
      await addReview(token!, reservation.restaurantId, {
        reservationId: reservation.id,
        rating,
        comment: comment || undefined,
      });
      setSuccess(true);
      setTimeout(() => navigate('/my-reservations'), 3000);
    } catch (err: any) {
      setReviewError(err.response?.data?.message || 'Грешка при изпращане на отзив.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <NavBar />
      <Container maxWidth="sm" sx={{ mt: 6 }}>
        {loading && <CircularProgress />}
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
            <Button size="small" sx={{ ml: 2 }} onClick={() => navigate('/my-reservations')}>
              Към моите резервации
            </Button>
          </Alert>
        )}

        {!loading && !error && alreadyReviewed && (
          <Alert severity="info">
            Вече сте оставили отзив за тази резервация.
            <Button size="small" sx={{ ml: 2 }} onClick={() => navigate('/my-reservations')}>
              Към моите резервации
            </Button>
          </Alert>
        )}

        {!loading && !error && !alreadyReviewed && reservation && !success && (
          <Paper elevation={3} sx={{ p: 4 }}>
            <Typography variant="h5" sx={{ fontWeight: 700, mb: 0.5, color: 'text.primary' }}>
              Оставете отзив
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              {reservation.restaurantName} · {reservation.reservationDate}
            </Typography>
            {reviewError && <Alert severity="error" sx={{ mb: 2 }}>{reviewError}</Alert>}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Box>
                <Typography variant="body2" sx={{ mb: 0.5, color: 'text.primary' }}>Оценка</Typography>
                <Rating value={rating} onChange={(_, v) => setRating(v ?? 1)} size="large" />
              </Box>
              <TextField
                label="Коментар (незадължително)"
                variant="filled"
                fullWidth
                multiline
                rows={4}
                value={comment}
                onChange={e => setComment(e.target.value)}
              />
              <Box sx={{ display: 'flex', gap: 2 }}>
                <Button variant="contained" onClick={handleSubmit} disabled={submitting}>
                  {submitting ? 'Изпращане...' : 'Изпрати отзив'}
                </Button>
                <Button variant="outlined" onClick={() => navigate('/my-reservations')}>
                  Отказ
                </Button>
              </Box>
            </Box>
          </Paper>
        )}

        {success && (
          <Alert severity="success">
            Отзивът е изпратен успешно! Пренасочване към моите резервации...
          </Alert>
        )}
      </Container>
    </>
  );
};

export default LeaveReviewPage;
