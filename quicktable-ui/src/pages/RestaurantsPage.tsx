import React, { useEffect, useState } from 'react';
import {
  Container, Typography, Card, CardContent, CardActions,
  Button, TextField, CircularProgress, Alert, CardMedia, Box
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getAllRestaurants } from '../api/restaurants';
import type { RestaurantResponse } from '../api/restaurants';
import NavBar from '../components/NavBar';

const RestaurantsPage: React.FC = () => {
  const { token } = useAuth();
  const navigate = useNavigate();
  const [restaurants, setRestaurants] = useState<RestaurantResponse[]>([]);
  const [filtered, setFiltered] = useState<RestaurantResponse[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getAllRestaurants(token || undefined)
      .then(data => {
        setRestaurants(data);
        setFiltered(data);
      })
      .catch(() => setError('Грешка при зареждане на ресторантите.'))
      .finally(() => setLoading(false));
  }, [token]);

  useEffect(() => {
    const q = search.toLowerCase();
    setFiltered(restaurants.filter(r =>
      r.name.toLowerCase().includes(q) || r.city.toLowerCase().includes(q)
    ));
  }, [search, restaurants]);

  return (
    <>
      <NavBar />
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <TextField
          label="Търси по име или град"
          variant="filled"
          fullWidth
          sx={{ mb: 3 }}
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        {loading && <CircularProgress />}
        {error && <Alert severity="error">{error}</Alert>}
        <Box sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr' },
          gap: 3,
        }}>
          {filtered.map(r => (
            <Card key={r.id} sx={{ display: 'flex', flexDirection: 'column' }}>
              <CardMedia
                component="img"
                height="180"
                image={r.imageUrl || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600&q=80'}
                alt={r.name}
              />
              <CardContent sx={{ flexGrow: 1 }}>
                <Typography variant="h6" sx={{ fontWeight: 700 }}>{r.name}</Typography>
                <Typography variant="body2" color="text.secondary">{r.city}, {r.country}</Typography>
                <Typography variant="body2" sx={{ mt: 1 }}>{r.address}</Typography>
                <Typography variant="body2" sx={{ mt: 1 }}>
                  {r.openingTime} – {r.closingTime}
                </Typography>
                {r.averageRating > 0 && (
                  <Typography variant="body2" sx={{ mt: 1 }}>
                    Рейтинг: {r.averageRating.toFixed(1)} ({r.reviewCount} отзива)
                  </Typography>
                )}
              </CardContent>
              <CardActions>
                <Button size="small" variant="contained" onClick={() => navigate(`/reserve/${r.id}`)}>
                  Резервирай
                </Button>
              </CardActions>
            </Card>
          ))}
        </Box>
      </Container>
    </>
  );
};

export default RestaurantsPage;
