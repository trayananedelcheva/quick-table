import axios from 'axios';

const USER_API = 'http://localhost:8081/api';

export interface UserProfile {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  role: string;
}

export const getMyProfile = async (token: string): Promise<UserProfile> => {
  const res = await axios.get(`${USER_API}/users/me`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};

export const updateProfile = async (
  token: string,
  firstName: string,
  lastName: string,
  phoneNumber: string
): Promise<UserProfile> => {
  const res = await axios.put(`${USER_API}/users/me/profile`, null, {
    headers: { Authorization: `Bearer ${token}` },
    params: { firstName, lastName, phoneNumber },
  });
  return res.data;
};

export const changePassword = async (token: string, currentPassword: string, newPassword: string) => {
  const res = await axios.put(`${USER_API}/users/me/password`, null, {
    headers: { Authorization: `Bearer ${token}` },
    params: { currentPassword, newPassword },
  });
  return res.data;
};

export const getAllUsers = async (token: string, role?: string): Promise<UserProfile[]> => {
  const res = await axios.get(`${USER_API}/users`, {
    headers: { Authorization: `Bearer ${token}` },
    params: role ? { role } : {},
  });
  return res.data;
};

export const updateUserRole = async (token: string, userId: number, role: string): Promise<UserProfile> => {
  const res = await axios.put(`${USER_API}/users/${userId}/role`, null, {
    headers: { Authorization: `Bearer ${token}` },
    params: { role },
  });
  return res.data;
};
