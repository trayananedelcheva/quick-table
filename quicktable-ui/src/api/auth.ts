import axios from 'axios';

const USER_API = 'http://localhost:8081/api';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phoneNumber?: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export const login = async (data: LoginRequest): Promise<AuthResponse> => {
  const res = await axios.post(`${USER_API}/auth/login`, data);
  return res.data;
};

export const register = async (data: RegisterRequest): Promise<AuthResponse> => {
  const res = await axios.post(`${USER_API}/auth/register`, data);
  return res.data;
};

export const forgotPassword = async (email: string): Promise<void> => {
  await axios.post(`${USER_API}/auth/forgot-password`, { email });
};

export const resetPassword = async (token: string, newPassword: string): Promise<void> => {
  await axios.post(`${USER_API}/auth/reset-password`, { token, newPassword });
};
