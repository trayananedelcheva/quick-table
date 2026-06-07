import axios from 'axios';

const RESTAURANT_API = 'http://localhost:8082/api';

export interface ReviewRequest {
  reservationId: number;
  rating: number;
  comment?: string;
}

export interface ReviewResponse {
  id: number;
  restaurantId: number;
  userId: number;
  reservationId: number;
  customerName: string;
  rating: number;
  comment?: string;
  createdAt: string;
}

export const addReview = async (
  token: string,
  restaurantId: number,
  data: ReviewRequest
): Promise<ReviewResponse> => {
  const res = await axios.post(`${RESTAURANT_API}/restaurants/${restaurantId}/reviews`, data, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const getReviews = async (restaurantId: number): Promise<ReviewResponse[]> => {
  const res = await axios.get(`${RESTAURANT_API}/restaurants/${restaurantId}/reviews`);
  return res.data;
};

export const getReviewByReservation = async (restaurantId: number, reservationId: number): Promise<ReviewResponse | null> => {
  try {
    const res = await axios.get(`${RESTAURANT_API}/restaurants/${restaurantId}/reviews/by-reservation/${reservationId}`);
    return res.data;
  } catch {
    return null;
  }
};
