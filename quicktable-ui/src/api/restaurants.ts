import axios from 'axios';

const RESTAURANT_API = 'http://localhost:8082/api';
const RESERVATION_API = 'http://localhost:8085/api';

export interface RestaurantResponse {
  id: number;
  name: string;
  description: string;
  address: string;
  city: string;
  country: string;
  phone: string;
  email: string;
  openingTime: string;
  closingTime: string;
  active: boolean;
  totalTables: number;
  availableTables: number;
  averageRating: number;
  reviewCount: number;
  imageUrl?: string;
  latitude?: number;
  longitude?: number;
}

export interface RestaurantCreateRequest {
  name: string;
  description?: string;
  address: string;
  city?: string;
  country?: string;
  phone?: string;
  email?: string;
  openingTime: string;
  closingTime: string;
}

export interface AdminRestaurantCreateRequest extends RestaurantCreateRequest {
  ownerId: number;
}

export interface RestaurantUpdateRequest {
  name: string;
  description?: string;
  address: string;
  city?: string;
  country?: string;
  phone?: string;
  email?: string;
  openingTime: string;
  closingTime: string;
}

export interface TableResponse {
  id: number;
  tableNumber: string;
  capacity: number;
  location: string;
  available: boolean;
}

export interface TableRequest {
  tableNumber: string;
  capacity: number;
  location: string;
}

export interface LocationAvailability {
  id: number;
  restaurantId: number;
  location: string;
  enabled: boolean;
}

export interface TimeSlotResponse {
  time: string;
  availableTables: number;
}

export const getAllRestaurants = async (token?: string): Promise<RestaurantResponse[]> => {
  const res = await axios.get(`${RESTAURANT_API}/restaurants`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  return res.data;
};

export const createRestaurant = async (token: string, data: RestaurantCreateRequest): Promise<RestaurantResponse> => {
  const res = await axios.post(`${RESTAURANT_API}/restaurants`, data, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const createRestaurantAsAdmin = async (token: string, data: AdminRestaurantCreateRequest): Promise<RestaurantResponse> => {
  const res = await axios.post(`${RESTAURANT_API}/restaurants/admin`, data, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const getMyRestaurants = async (token: string): Promise<RestaurantResponse[]> => {
  const res = await axios.get(`${RESTAURANT_API}/restaurants/my`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const updateRestaurant = async (
  token: string,
  id: number,
  data: RestaurantUpdateRequest
): Promise<RestaurantResponse> => {
  const res = await axios.put(`${RESTAURANT_API}/restaurants/${id}`, data, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const deleteRestaurant = async (token: string, id: number): Promise<void> => {
  await axios.delete(`${RESTAURANT_API}/restaurants/${id}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
};

export const getRestaurantTables = async (token: string, id: number): Promise<TableResponse[]> => {
  const res = await axios.get(`${RESTAURANT_API}/restaurants/${id}/tables`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const addTable = async (token: string, restaurantId: number, data: TableRequest): Promise<TableResponse> => {
  const res = await axios.post(`${RESTAURANT_API}/restaurants/${restaurantId}/tables`, data, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const updateTable = async (token: string, restaurantId: number, tableId: number, data: TableRequest): Promise<TableResponse> => {
  const res = await axios.put(`${RESTAURANT_API}/restaurants/${restaurantId}/tables/${tableId}`, data, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const deleteTable = async (token: string, restaurantId: number, tableId: number): Promise<void> => {
  await axios.delete(`${RESTAURANT_API}/restaurants/${restaurantId}/tables/${tableId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
};

export const updateTableAvailability = async (
  token: string,
  restaurantId: number,
  tableNumber: string,
  available: boolean
): Promise<TableResponse> => {
  const res = await axios.put(
    `${RESTAURANT_API}/restaurants/${restaurantId}/tables/${tableNumber}/availability`,
    null,
    { headers: { Authorization: `Bearer ${token}` }, params: { available } }
  );
  return res.data;
};

export const getLocationAvailability = async (token: string, id: number): Promise<LocationAvailability[]> => {
  const res = await axios.get(`${RESTAURANT_API}/restaurants/${id}/locations`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const toggleLocationAvailability = async (
  token: string,
  restaurantId: number,
  location: string,
  enabled: boolean
): Promise<LocationAvailability> => {
  const res = await axios.put(
    `${RESTAURANT_API}/restaurants/${restaurantId}/locations/${location}/toggle`,
    null,
    { headers: { Authorization: `Bearer ${token}` }, params: { enabled } }
  );
  return res.data;
};

export const getAvailableTimeSlots = async (
  token: string | undefined,
  restaurantId: number,
  date: string,
  guestsCount: number
): Promise<TimeSlotResponse[]> => {
  const res = await axios.get(
    `${RESERVATION_API}/reservations/restaurant/${restaurantId}/available-slots`,
    {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      params: { date, guestsCount },
    }
  );
  return res.data;
};
