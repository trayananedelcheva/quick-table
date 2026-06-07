import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const HomePage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (user?.role === 'RESTAURANT_ADMIN') {
      navigate('/admin/restaurant', { replace: true });
    } else if (user?.role === 'SYSTEM_ADMIN') {
      navigate('/admin/users', { replace: true });
    } else {
      navigate('/', { replace: true });
    }
  }, [user, navigate]);

  return null;
};

export default HomePage;
