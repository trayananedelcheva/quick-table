import React, { useEffect, useState } from 'react';
import {
  Container, Typography, Box, CircularProgress, Alert,
  Button, Divider, Rating, Avatar, Paper
} from '@mui/material';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getAllRestaurants } from '../api/restaurants';
import type { RestaurantResponse } from '../api/restaurants';
import { getReviews } from '../api/reviews';
import type { ReviewResponse } from '../api/reviews';
import NavBar from '../components/NavBar';

const DEFAULT_IMAGE = 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80';

const RestaurantDetailPage: React.FC = () => {
  const { restaurantId } = useParams<{ restaurantId: string }>();
  const { token } = useAuth();
  const navigate = useNavigate();
  const [restaurant, setRestaurant] = useState<RestaurantResponse | null>(null);
  const [reviews, setReviews] = useState<ReviewResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([
      getAllRestaurants(token!),
      getReviews(Number(restaurantId)),
    ])
      .then(([restaurants, revs]) => {
        const r = restaurants.find(r => r.id === Number(restaurantId));
        setRestaurant(r || null);
        setReviews(revs);
      })
      .catch(() => setError('Грешка при зареждане.'))
      .finally(() => setLoading(false));
  }, [restaurantId, token]);

  return (
    <>
      <NavBar />
      <Container maxWidth="md" sx={{ mt: 4, pb: 6 }}>
        {loading && <CircularProgress />}
        {error && <Alert severity="error">{error}</Alert>}
        {restaurant && (
          <>
            <Box
              component="img"
              src={restaurant.imageUrl || DEFAULT_IMAGE}
              alt={restaurant.name}
              sx={{ width: '100%', height: 320, objectFit: 'cover', borderRadius: 2, mb: 3 }}
            />
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 2 }}>
              <Box>
                <Typography variant="h4" sx={{ fontWeight: 700 }}>{restaurant.name}</Typography>
                <Typography variant="body1" color="text.secondary">{restaurant.city}, {restaurant.country}</Typography>
              </Box>
              <Button variant="contained" size="large" onClick={() => navigate(`/reserve/${restaurant.id}`)}>
                Резервирай
              </Button>
            </Box>

            {restaurant.averageRating > 0 && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
                <Rating value={restaurant.averageRating} precision={0.1} readOnly size="small" />
                <Typography variant="body2">{restaurant.averageRating.toFixed(1)} ({restaurant.reviewCount} отзива)</Typography>
              </Box>
            )}

            <Divider sx={{ my: 3 }} />

            <Box sx={{ display: 'flex', gap: 4, flexWrap: 'wrap', mb: 3 }}>
              <Box>
                <Typography variant="overline" color="text.secondary">Адрес</Typography>
                <Typography variant="body1">{restaurant.address}</Typography>
              </Box>
              <Box>
                <Typography variant="overline" color="text.secondary">Работно време</Typography>
                <Typography variant="body1">{restaurant.openingTime} – {restaurant.closingTime}</Typography>
              </Box>
              {restaurant.phone && (
                <Box>
                  <Typography variant="overline" color="text.secondary">Телефон</Typography>
                  <Typography variant="body1">{restaurant.phone}</Typography>
                </Box>
              )}
              {restaurant.email && (
                <Box>
                  <Typography variant="overline" color="text.secondary">Имейл</Typography>
                  <Typography variant="body1">{restaurant.email}</Typography>
                </Box>
              )}
            </Box>

            {restaurant.description && (
              <>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>За ресторанта</Typography>
                <Typography variant="body1" sx={{ mb: 3 }}>{restaurant.description}</Typography>
              </>
            )}

            <Divider sx={{ my: 3 }} />

            <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
              Отзиви {reviews.length > 0 && `(${reviews.length})`}
            </Typography>
            {reviews.length === 0 && (
              <Typography color="text.secondary">Все още няма отзиви.</Typography>
            )}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              {reviews.map(r => (
                <Paper key={r.id} variant="outlined" sx={{ p: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <Avatar sx={{ width: 32, height: 32, fontSize: 14 }}>
                      {r.customerName.charAt(0)}
                    </Avatar>
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 700 }}>{r.customerName}</Typography>
                      <Rating value={r.rating} readOnly size="small" />
                    </Box>
                  </Box>
                  {r.comment && <Typography variant="body2">{r.comment}</Typography>}
                </Paper>
              ))}
            </Box>
          </>
        )}
      </Container>
    </>
  );
};

export default RestaurantDetailPage;
