import axios from 'axios';

const RESERVATION_API = 'http://localhost:8085/api';

export interface ReservationRequest {
  restaurantId: number;
  reservationDate: string;
  reservationTime: string;
  guestsCount: number;
  preferredLocation?: string;
  specialRequests?: string;
}

export interface ReservationResponse {
  id: number;
  restaurantId: number;
  restaurantName: string;
  restaurantImageUrl?: string;
  reservationDate: string;
  reservationTime: string;
  guestsCount: number;
  numberOfGuests: number;
  customerName: string;
  customerEmail: string;
  status: string;
  specialRequests?: string;
}

export const createReservation = async (
  token: string,
  data: ReservationRequest
): Promise<ReservationResponse> => {
  const res = await axios.post(`${RESERVATION_API}/reservations`, data, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const getMyReservations = async (token: string): Promise<ReservationResponse[]> => {
  const res = await axios.get(`${RESERVATION_API}/reservations/my`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const getRestaurantReservations = async (
  token: string,
  restaurantId: number,
  date?: string,
  status?: string
): Promise<ReservationResponse[]> => {
  const res = await axios.get(`${RESERVATION_API}/reservations/restaurant/${restaurantId}`, {
    headers: { Authorization: `Bearer ${token}` },
    params: { ...(date && { date }), ...(status && { status }) },
  });
  return res.data;
};

export const completeReservation = async (token: string, id: number): Promise<ReservationResponse> => {
  const res = await axios.patch(`${RESERVATION_API}/reservations/${id}/complete`, {}, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const rejectReservation = async (token: string, id: number, reason: string): Promise<ReservationResponse> => {
  const res = await axios.patch(`${RESERVATION_API}/reservations/${id}/reject`, { reason }, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const markNoShow = async (token: string, id: number): Promise<ReservationResponse> => {
  const res = await axios.patch(`${RESERVATION_API}/reservations/${id}/no-show`, {}, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const getAllReservations = async (token: string): Promise<ReservationResponse[]> => {
  const res = await axios.get(`${RESERVATION_API}/reservations`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const cancelReservation = async (token: string, id: number): Promise<ReservationResponse> => {
  const res = await axios.delete(`${RESERVATION_API}/reservations/${id}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};
