import React, { useEffect, useState } from 'react';
import {
  Container, Typography, Box, CircularProgress, Alert,
  Table, TableBody, TableCell, TableHead, TableRow,
  Paper, TextField, MenuItem, Chip, Select, FormControl,
  InputLabel, Button, Dialog, DialogTitle, DialogContent, DialogActions
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useAuth } from '../context/AuthContext';
import { getAllUsers, updateUserRole } from '../api/profile';
import type { UserProfile } from '../api/profile';
import { createRestaurantAsAdmin } from '../api/restaurants';
import type { AdminRestaurantCreateRequest } from '../api/restaurants';
import axios from 'axios';
import NavBar from '../components/NavBar';

const ROLES = ['CLIENT', 'RESTAURANT_ADMIN', 'SYSTEM_ADMIN'];

const ROLE_LABELS: Record<string, string> = {
  CLIENT: 'Клиент',
  RESTAURANT_ADMIN: 'Администратор на ресторант',
  SYSTEM_ADMIN: 'Системен администратор',
};

const ROLE_COLORS: Record<string, 'default' | 'primary' | 'error'> = {
  CLIENT: 'default',
  RESTAURANT_ADMIN: 'primary',
  SYSTEM_ADMIN: 'error',
};

const emptyForm: AdminRestaurantCreateRequest = {
  ownerId: 0,
  name: '',
  description: '',
  address: '',
  city: '',
  country: '',
  phone: '',
  email: '',
  openingTime: '09:00',
  closingTime: '22:00',
};

const AdminUsersPage: React.FC = () => {
  const { token } = useAuth();
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [filtered, setFiltered] = useState<UserProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [updating, setUpdating] = useState<number | null>(null);

  const [createDialog, setCreateDialog] = useState(false);
  const [createForm, setCreateForm] = useState<AdminRestaurantCreateRequest>(emptyForm);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');
  const [createSuccess, setCreateSuccess] = useState('');
  const [imageFile, setImageFile] = useState<File | null>(null);

  const restaurantAdmins = users.filter(u => u.role === 'RESTAURANT_ADMIN');

  useEffect(() => {
    getAllUsers(token!)
      .then(data => { setUsers(data); setFiltered(data); })
      .catch(() => setError('Грешка при зареждане на потребителите.'))
      .finally(() => setLoading(false));
  }, [token]);

  useEffect(() => {
    let result = users;
    if (roleFilter) result = result.filter(u => u.role === roleFilter);
    if (search) {
      const q = search.toLowerCase();
      result = result.filter(u =>
        u.email.toLowerCase().includes(q) ||
        u.firstName.toLowerCase().includes(q) ||
        u.lastName.toLowerCase().includes(q)
      );
    }
    setFiltered(result);
  }, [search, roleFilter, users]);

  const handleRoleChange = async (userId: number, newRole: string) => {
    setUpdating(userId);
    try {
      const updated = await updateUserRole(token!, userId, newRole);
      setUsers(prev => prev.map(u => u.id === userId ? updated : u));
    } catch (err: any) {
      setError(err.response?.data?.message || 'Грешка при смяна на роля.');
    } finally {
      setUpdating(null);
    }
  };

  const handleCreate = async () => {
    setCreating(true);
    setCreateError('');
    try {
      const created = await createRestaurantAsAdmin(token!, createForm);
      if (imageFile) {
        const formData = new FormData();
        formData.append('file', imageFile);
        try {
          await axios.post(
            `http://localhost:8082/api/restaurants/${created.id}/image`,
            formData,
            { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data' } }
          );
        } catch {}
      }
      setCreateSuccess('Ресторантът е създаден успешно.');
      setCreateDialog(false);
      setCreateForm(emptyForm);
      setImageFile(null);
    } catch (err: any) {
      setCreateError(err.response?.data?.message || 'Грешка при създаване.');
    } finally {
      setCreating(false);
    }
  };

  return (
    <>
      <NavBar />
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>Управление на потребители</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateDialog(true)}>
            Нов ресторант
          </Button>
        </Box>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {createSuccess && <Alert severity="success" sx={{ mb: 2 }}>{createSuccess}</Alert>}

        <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
          <TextField label="Търси по име или имейл" variant="filled" value={search} onChange={e => setSearch(e.target.value)} sx={{ flex: 1 }} />
          <TextField label="Филтър по роля" variant="filled" select value={roleFilter} onChange={e => setRoleFilter(e.target.value)} sx={{ width: 240 }}>
            <MenuItem value="">Всички</MenuItem>
            {ROLES.map(r => <MenuItem key={r} value={r}>{ROLE_LABELS[r]}</MenuItem>)}
          </TextField>
        </Box>

        {loading ? <CircularProgress /> : (
          <Paper elevation={3}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Имейл</TableCell>
                  <TableCell>Име</TableCell>
                  <TableCell>Телефон</TableCell>
                  <TableCell>Текуща роля</TableCell>
                  <TableCell>Смяна на роля</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filtered.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} align="center">
                      <Typography color="text.secondary">Няма намерени потребители.</Typography>
                    </TableCell>
                  </TableRow>
                )}
                {filtered.map(u => (
                  <TableRow key={u.id}>
                    <TableCell>{u.email}</TableCell>
                    <TableCell>{u.firstName} {u.lastName}</TableCell>
                    <TableCell>{u.phoneNumber || '—'}</TableCell>
                    <TableCell>
                      <Chip label={ROLE_LABELS[u.role] || u.role} color={ROLE_COLORS[u.role] || 'default'} size="small" />
                    </TableCell>
                    <TableCell>
                      <FormControl size="small" variant="outlined" disabled={updating === u.id}>
                        <InputLabel>Роля</InputLabel>
                        <Select label="Роля" value={u.role} onChange={e => handleRoleChange(u.id, e.target.value)} sx={{ minWidth: 200 }}>
                          {ROLES.map(r => <MenuItem key={r} value={r}>{ROLE_LABELS[r]}</MenuItem>)}
                        </Select>
                      </FormControl>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        )}
      </Container>

      <Dialog open={createDialog} onClose={() => setCreateDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Нов ресторант (от името на admin)</DialogTitle>
        <DialogContent>
          {createError && <Alert severity="error" sx={{ mb: 2 }}>{createError}</Alert>}
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <TextField
              label="Собственик (Restaurant Admin) *"
              variant="filled"
              fullWidth
              select
              value={createForm.ownerId || ''}
              onChange={e => setCreateForm(f => ({ ...f, ownerId: Number(e.target.value) }))}
            >
              {restaurantAdmins.map(u => (
                <MenuItem key={u.id} value={u.id}>{u.firstName} {u.lastName} — {u.email}</MenuItem>
              ))}
            </TextField>
            <TextField label="Име *" variant="filled" fullWidth value={createForm.name} onChange={e => setCreateForm(f => ({ ...f, name: e.target.value }))} />
            <TextField label="Описание" variant="filled" fullWidth multiline rows={2} value={createForm.description} onChange={e => setCreateForm(f => ({ ...f, description: e.target.value }))} />
            <TextField label="Адрес *" variant="filled" fullWidth value={createForm.address} onChange={e => setCreateForm(f => ({ ...f, address: e.target.value }))} />
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField label="Град" variant="filled" fullWidth value={createForm.city} onChange={e => setCreateForm(f => ({ ...f, city: e.target.value }))} />
              <TextField label="Държава" variant="filled" fullWidth value={createForm.country} onChange={e => setCreateForm(f => ({ ...f, country: e.target.value }))} />
            </Box>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField label="Телефон" variant="filled" fullWidth value={createForm.phone} onChange={e => setCreateForm(f => ({ ...f, phone: e.target.value }))} />
              <TextField label="Имейл" variant="filled" fullWidth value={createForm.email} onChange={e => setCreateForm(f => ({ ...f, email: e.target.value }))} />
            </Box>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField label="Отваряне" variant="filled" fullWidth value={createForm.openingTime} onChange={e => setCreateForm(f => ({ ...f, openingTime: e.target.value }))} />
              <TextField label="Затваряне" variant="filled" fullWidth value={createForm.closingTime} onChange={e => setCreateForm(f => ({ ...f, closingTime: e.target.value }))} />
            </Box>
            <Box>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Снимка (незадължително)</Typography>
              <Button variant="outlined" component="label" fullWidth>
                {imageFile ? imageFile.name : 'Избери снимка'}
                <input type="file" accept="image/*" hidden onChange={e => setImageFile(e.target.files?.[0] || null)} />
              </Button>
            </Box>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setCreateDialog(false); setCreateForm(emptyForm); setCreateError(''); }}>Отказ</Button>
          <Button variant="contained" onClick={handleCreate} disabled={creating || !createForm.name || !createForm.address || !createForm.ownerId}>
            {creating ? 'Създаване...' : 'Създай'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default AdminUsersPage;
