import React, { useEffect, useState } from 'react';
import {
  Box, Container, Typography, TextField, Button,
  MenuItem, CircularProgress, Alert, Paper, Rating, Divider, Chip, Avatar
} from '@mui/material';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import PhoneIcon from '@mui/icons-material/Phone';
import EmailIcon from '@mui/icons-material/Email';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getAllRestaurants, getAvailableTimeSlots } from '../api/restaurants';
import type { RestaurantResponse } from '../api/restaurants';
import { createReservation } from '../api/reservations';
import { getReviews } from '../api/reviews';
import type { ReviewResponse } from '../api/reviews';
import NavBar from '../components/NavBar';

interface ReservationLocationState {
  returnTo?: string;
  date?: string;
  guests?: number;
  location2?: string;
  slots?: { time: string }[];
  selectedTime?: string;
  specialRequests?: string;
}

const DEFAULT_IMAGE = 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800&q=80';

const LOCATIONS = [
  { value: '', label: 'Без предпочитание' },
  { value: 'INSIDE', label: 'Вътре' },
  { value: 'SUMMER_GARDEN', label: 'Лятна градина' },
  { value: 'WINTER_GARDEN', label: 'Зимна градина' },
];

const ReservationPage: React.FC = () => {
  const { restaurantId } = useParams<{ restaurantId: string }>();
  const { token } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const savedState = location.state as ReservationLocationState;

  const [restaurant, setRestaurant] = useState<RestaurantResponse | null>(null);
  const [reviews, setReviews] = useState<ReviewResponse[]>([]);
  const [date, setDate] = useState(savedState?.date || '');
  const [guests, setGuests] = useState(savedState?.guests || 2);
  const [location2, setLocation2] = useState(savedState?.location2 || '');
  const [slots, setSlots] = useState<{ time: string }[]>(savedState?.slots || []);
  const [selectedTime, setSelectedTime] = useState(savedState?.selectedTime || '');
  const [specialRequests, setSpecialRequests] = useState(savedState?.specialRequests || '');
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    getAllRestaurants(token || undefined).then(data => {
      const r = data.find(r => r.id === Number(restaurantId));
      setRestaurant(r || null);
    });
    getReviews(Number(restaurantId)).then(setReviews).catch(() => {});
  }, [restaurantId, token]);

  const handleLoadSlots = async () => {
    if (!date || !guests) return;
    setLoadingSlots(true);
    setSlots([]);
    setSelectedTime('');
    setError('');
    try {
      const data = await getAvailableTimeSlots(token || undefined, Number(restaurantId), date, guests);
      setSlots(data);
      if (data.length === 0) setError('Няма свободни часове за избраната дата.');
    } catch {
      setError('Грешка при зареждане на часовете.');
    } finally {
      setLoadingSlots(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTime) { setError('Изберете час.'); return; }
    setSubmitting(true);
    setError('');
    try {
      await createReservation(token!, {
        restaurantId: Number(restaurantId),
        reservationDate: date,
        reservationTime: selectedTime,
        guestsCount: guests,
        preferredLocation: location2 || undefined,
        specialRequests: specialRequests || undefined,
      });
      setSuccess(true);
      setTimeout(() => navigate('/my-reservations'), 2000);
    } catch (err: any) {
      setError(err.response?.data?.message || err.response?.data?.error || JSON.stringify(err.response?.data) || 'Грешка при създаване на резервация.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <NavBar />
      <Container maxWidth="lg" sx={{ mt: 4, pb: 6 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)} sx={{ mb: 2 }}>
          Назад
        </Button>

        <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start', flexWrap: 'wrap' }}>

          {/* Ляво — информация за ресторанта */}
          <Box sx={{ flex: 1, minWidth: 300 }}>
            <Box
              component="img"
              src={restaurant?.imageUrl || DEFAULT_IMAGE}
              alt={restaurant?.name}
              sx={{ width: '100%', height: 280, objectFit: 'cover', borderRadius: 2, mb: 3 }}
            />
            <Typography variant="h4" sx={{ fontWeight: 700, mb: 0.5 }}>
              {restaurant?.name}
            </Typography>
            {restaurant && restaurant.averageRating > 0 && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                <Rating value={restaurant.averageRating} precision={0.1} readOnly size="small" />
                <Typography variant="body2" color="text.secondary">
                  {restaurant.averageRating.toFixed(1)} ({restaurant.reviewCount} отзива)
                </Typography>
              </Box>
            )}
            <Divider sx={{ mb: 2 }} />
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <LocationOnIcon fontSize="small" color="action" />
                <Typography variant="body2">{restaurant?.address}, {restaurant?.city}</Typography>
                {restaurant && (restaurant.latitude || restaurant.address) && (
                  <a
                    href={
                      restaurant.latitude && restaurant.longitude
                        ? `https://www.google.com/maps?q=${restaurant.latitude},${restaurant.longitude}`
                        : `https://www.google.com/maps/search/${encodeURIComponent(`${restaurant.address}, ${restaurant.city}`)}`
                    }
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{ fontSize: 12, marginLeft: 4, whiteSpace: 'nowrap' }}
                  >
                    Виж в Google Maps ↗
                  </a>
                )}
              </Box>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <AccessTimeIcon fontSize="small" color="action" />
                <Typography variant="body2">{restaurant?.openingTime} – {restaurant?.closingTime}</Typography>
              </Box>
              {restaurant?.phone && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <PhoneIcon fontSize="small" color="action" />
                  <Typography variant="body2">{restaurant.phone}</Typography>
                </Box>
              )}
              {restaurant?.email && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <EmailIcon fontSize="small" color="action" />
                  <Typography variant="body2">{restaurant.email}</Typography>
                </Box>
              )}
            </Box>
            {restaurant?.description && (
              <>
                <Divider sx={{ my: 2 }} />
                <Typography variant="body2" color="text.secondary">{restaurant.description}</Typography>
              </>
            )}

            {reviews.length > 0 && (
              <>
                <Divider sx={{ my: 2 }} />
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                  Отзиви ({reviews.length})
                </Typography>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  {reviews.map(r => (
                    <Paper key={r.id} variant="outlined" sx={{ p: 2 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                        <Avatar sx={{ width: 32, height: 32, fontSize: 14 }}>
                          {r.customerName.charAt(0)}
                        </Avatar>
                        <Box>
                          <Typography variant="body2" sx={{ fontWeight: 700 }}>{r.customerName}</Typography>
                          <Rating value={r.rating} readOnly size="small" />
                        </Box>
                      </Box>
                      {r.comment && <Typography variant="body2" color="text.secondary">{r.comment}</Typography>}
                    </Paper>
                  ))}
                </Box>
              </>
            )}
          </Box>

          {/* Дясно — форма за резервация */}
          <Paper elevation={3} sx={{ width: 380, flexShrink: 0, p: 4 }}>
            <Typography variant="h6" sx={{ fontWeight: 700, textAlign: 'center', mb: 0.5 }}>
              НАПРАВЕТЕ
            </Typography>
            <Typography variant="h5" sx={{ fontWeight: 900, textAlign: 'center', mb: 3 }}>
              РЕЗЕРВАЦИЯ
            </Typography>

            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            {success && <Alert severity="success" sx={{ mb: 2 }}>Резервацията е създадена успешно!</Alert>}

            <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <TextField
                label="Дата"
                type="date"
                variant="filled"
                fullWidth
                required
                value={date}
                onChange={e => setDate(e.target.value)}
                slotProps={{
                  inputLabel: { shrink: true },
                  htmlInput: { min: new Date().toISOString().split('T')[0] },
                }}
              />
              <TextField
                label="Брой гости"
                type="number"
                variant="filled"
                fullWidth
                required
                value={guests}
                onChange={e => setGuests(Number(e.target.value))}
                slotProps={{ htmlInput: { min: 1, max: 20 } }}
              />
              <TextField
                label="Предпочитана локация"
                variant="filled"
                fullWidth
                select
                value={location2}
                onChange={e => setLocation2(e.target.value)}
              >
                {LOCATIONS.map(l => (
                  <MenuItem key={l.value} value={l.value}>{l.label}</MenuItem>
                ))}
              </TextField>

              <Button
                variant="outlined"
                fullWidth
                onClick={handleLoadSlots}
                disabled={!date || loadingSlots}
              >
                {loadingSlots ? <CircularProgress size={20} /> : 'Виж свободни часове'}
              </Button>

              {slots.length > 0 && (
                <>
                  <Typography variant="body2" color="text.secondary">Изберете час:</Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                    {slots.map(s => (
                      <Chip
                        key={s.time}
                        label={s.time}
                        onClick={() => setSelectedTime(s.time)}
                        color={selectedTime === s.time ? 'primary' : 'default'}
                        variant={selectedTime === s.time ? 'filled' : 'outlined'}
                        clickable
                      />
                    ))}
                  </Box>
                </>
              )}

              <TextField
                label="Специални изисквания (незадължително)"
                variant="filled"
                fullWidth
                multiline
                rows={3}
                value={specialRequests}
                onChange={e => setSpecialRequests(e.target.value)}
              />
              {!token && selectedTime && (
                <Alert severity="info" sx={{ mt: 1 }}>
                  Трябва да влезете в профила си, за да направите резервация.
                </Alert>
              )}
              {!token ? (
                <Button
                  variant="contained"
                  fullWidth
                  size="large"
                  disabled={!selectedTime}
                  onClick={() => navigate('/login', {
                    state: {
                      returnTo: `/reserve/${restaurantId}`,
                      date,
                      guests,
                      location2,
                      slots,
                      selectedTime,
                      specialRequests,
                    }
                  })}
                >
                  Влез, за да резервираш
                </Button>
              ) : (
                <Button
                  type="submit"
                  variant="contained"
                  fullWidth
                  size="large"
                  disabled={submitting || !selectedTime}
                >
                  {submitting ? 'Изпращане...' : 'Резервирай'}
                </Button>
              )}
            </Box>
          </Paper>
        </Box>
      </Container>
    </>
  );
};

export default ReservationPage;
