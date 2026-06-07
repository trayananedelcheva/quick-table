import React, { useEffect, useState } from 'react';
import {
  Container, Typography, Box, TextField, Button,
  CircularProgress, Alert, Paper, Divider, MenuItem,
  Table, TableBody, TableCell, TableHead, TableRow,
  Switch, FormControlLabel, Chip, Dialog, DialogTitle,
  DialogContent, DialogActions, IconButton
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import { useAuth } from '../context/AuthContext';
import {
  getMyRestaurants, updateRestaurant, deleteRestaurant,
  getRestaurantTables, addTable, updateTable, deleteTable, updateTableAvailability,
  getLocationAvailability, toggleLocationAvailability,
  createRestaurant,
} from '../api/restaurants';
import type {
  RestaurantResponse, RestaurantUpdateRequest, RestaurantCreateRequest,
  TableResponse, TableRequest, LocationAvailability
} from '../api/restaurants';
import NavBar from '../components/NavBar';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

const LOCATION_LABELS: Record<string, string> = {
  INSIDE: 'Вътре',
  SUMMER_GARDEN: 'Лятна градина',
  WINTER_GARDEN: 'Зимна градина',
};

const toForm = (r: RestaurantResponse): RestaurantUpdateRequest => ({
  name: r.name,
  description: r.description || '',
  address: r.address,
  city: r.city || '',
  country: r.country || '',
  phone: r.phone || '',
  email: r.email || '',
  openingTime: r.openingTime,
  closingTime: r.closingTime,
});

const AdminRestaurantPage: React.FC = () => {
  const { token } = useAuth();
  const navigate = useNavigate();
  const [restaurants, setRestaurants] = useState<RestaurantResponse[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [form, setForm] = useState<RestaurantUpdateRequest | null>(null);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [uploading, setUploading] = useState(false);

  // Tables state
  const [tables, setTables] = useState<TableResponse[]>([]);
  const [tableForm, setTableForm] = useState<TableRequest>({ tableNumber: '', capacity: 2, location: 'INSIDE' });
  const [addingTable, setAddingTable] = useState(false);
  const [tableError, setTableError] = useState('');
  const [editTableDialog, setEditTableDialog] = useState<{ open: boolean; table: TableResponse | null }>({ open: false, table: null });
  const [editTableForm, setEditTableForm] = useState<TableRequest>({ tableNumber: '', capacity: 2, location: 'INSIDE' });
  const [deleteTableDialog, setDeleteTableDialog] = useState<{ open: boolean; table: TableResponse | null }>({ open: false, table: null });

  // Locations state
  const [locations, setLocations] = useState<LocationAvailability[]>([]);

  // Delete dialog
  const [deleteDialog, setDeleteDialog] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const restaurant = restaurants.find(r => r.id === selectedId) || null;

  const emptyCreateForm: RestaurantCreateRequest = { name: '', description: '', address: '', city: '', country: '', phone: '', email: '', openingTime: '09:00', closingTime: '22:00' };
  const [createDialog, setCreateDialog] = useState(false);
  const [createForm, setCreateForm] = useState<RestaurantCreateRequest>(emptyCreateForm);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');
  const [imageFile, setImageFile] = useState<File | null>(null);

  useEffect(() => {
    getMyRestaurants(token!)
      .then(data => {
        setRestaurants(data);
        if (data.length > 0) {
          setSelectedId(data[0].id);
          setForm(toForm(data[0]));
        }
      })
      .catch(() => setError('Грешка при зареждане на ресторантите.'))
      .finally(() => setLoading(false));
  }, [token]);

  useEffect(() => {
    if (!selectedId) return;
    getRestaurantTables(token!, selectedId).then(setTables).catch(() => {});
    getLocationAvailability(token!, selectedId).then(setLocations).catch(() => {});
  }, [selectedId, token]);

  const handleSelectRestaurant = (id: number) => {
    const r = restaurants.find(r => r.id === id);
    if (r) {
      setSelectedId(id);
      setForm(toForm(r));
      setEditing(false);
      setSuccess('');
      setError('');
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm(f => f ? { ...f, [e.target.name]: e.target.value } : f);
  };

  const handleSave = async () => {
    if (!restaurant || !form) return;
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const updated = await updateRestaurant(token!, restaurant.id, form);
      setRestaurants(rs => rs.map(r => r.id === updated.id ? updated : r));
      setEditing(false);
      setSuccess('Информацията е обновена успешно.');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Грешка при запис.');
    } finally {
      setSaving(false);
    }
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !restaurant) return;
    const formData = new FormData();
    formData.append('file', file);
    setUploading(true);
    try {
      const res = await axios.post(
        `http://localhost:8082/api/restaurants/${restaurant.id}/image`,
        formData,
        { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data' } }
      );
      setRestaurants(rs => rs.map(r => r.id === res.data.id ? res.data : r));
      setSuccess('Снимката е качена успешно.');
    } catch {
      setError('Грешка при качване на снимка.');
    } finally {
      setUploading(false);
      e.target.value = '';
    }
  };

  const handleAddTable = async () => {
    if (!restaurant) return;
    setTableError('');
    try {
      const t = await addTable(token!, restaurant.id, tableForm);
      setTables(prev => [...prev, t]);
      setTableForm({ tableNumber: '', capacity: 2, location: 'INSIDE' });
      setAddingTable(false);
    } catch (err: any) {
      setTableError(err.response?.data?.message || 'Грешка при добавяне на маса.');
    }
  };

  const handleToggleTable = async (tableNumber: string, current: boolean) => {
    if (!restaurant) return;
    try {
      const updated = await updateTableAvailability(token!, restaurant.id, tableNumber, !current);
      setTables(prev => prev.map(t => t.tableNumber === tableNumber ? updated : t));
    } catch {}
  };

  const handleToggleLocation = async (location: string, current: boolean) => {
    if (!restaurant) return;
    try {
      await toggleLocationAvailability(token!, restaurant.id, location, !current);
      setLocations(prev => prev.map(l => l.location === location ? { ...l, enabled: !current } : l));
    } catch {}
  };

  const handleEditTable = async () => {
    if (!restaurant || !editTableDialog.table) return;
    try {
      const updated = await updateTable(token!, restaurant.id, editTableDialog.table.id, editTableForm);
      setTables(prev => prev.map(t => t.id === updated.id ? updated : t));
      setEditTableDialog({ open: false, table: null });
    } catch (err: any) {
      setTableError(err.response?.data?.message || 'Грешка при редактиране.');
    }
  };

  const handleDeleteTable = async () => {
    if (!restaurant || !deleteTableDialog.table) return;
    try {
      await deleteTable(token!, restaurant.id, deleteTableDialog.table.id);
      setTables(prev => prev.filter(t => t.id !== deleteTableDialog.table!.id));
      setDeleteTableDialog({ open: false, table: null });
    } catch (err: any) {
      setTableError(err.response?.data?.message || 'Грешка при изтриване.');
    }
  };

  const handleDelete = async () => {
    if (!restaurant) return;
    setDeleting(true);
    try {
      await deleteRestaurant(token!, restaurant.id);
      setRestaurants(prev => prev.filter(r => r.id !== restaurant.id));
      setDeleteDialog(false);
      navigate('/admin/restaurant');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Грешка при изтриване.');
      setDeleteDialog(false);
    } finally {
      setDeleting(false);
    }
  };

  const handleCreate = async () => {
    setCreating(true);
    setCreateError('');
    try {
      const created = await createRestaurant(token!, createForm);

      if (imageFile) {
        const formData = new FormData();
        formData.append('file', imageFile);
        try {
          const imgRes = await axios.post(
            `http://localhost:8082/api/restaurants/${created.id}/image`,
            formData,
            { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data' } }
          );
          created.imageUrl = imgRes.data.imageUrl;
        } catch {}
      }

      setRestaurants(prev => [...prev, created]);
      setSelectedId(created.id);
      setForm(toForm(created));
      setCreateDialog(false);
      setCreateForm(emptyCreateForm);
      setImageFile(null);
      setSuccess('Ресторантът е създаден успешно.');
    } catch (err: any) {
      setCreateError(err.response?.data?.message || 'Грешка при създаване.');
    } finally {
      setCreating(false);
    }
  };

  return (
    <>
      <NavBar />
      <Container maxWidth="md" sx={{ mt: 4, pb: 6 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>Моите ресторанти</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateDialog(true)}>
            Нов ресторант
          </Button>
        </Box>
        {loading && <CircularProgress />}
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}
        {!loading && restaurants.length === 0 && <Alert severity="info">Нямате регистриран ресторант.</Alert>}

        {restaurants.length > 1 && (
          <TextField label="Избери ресторант" variant="filled" fullWidth select sx={{ mb: 3 }}
            value={selectedId ?? ''} onChange={e => handleSelectRestaurant(Number(e.target.value))}>
            {restaurants.map(r => <MenuItem key={r.id} value={r.id}>{r.name} — {r.city}</MenuItem>)}
          </TextField>
        )}

        {restaurant && form && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>

            {/* Основна информация */}
            <Paper elevation={3} sx={{ p: 4 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h5" sx={{ fontWeight: 700 }}>{restaurant.name}</Typography>
                <Box sx={{ display: 'flex', gap: 1 }}>
                  <Button variant="outlined" component="label" disabled={uploading}>
                    {uploading ? 'Качване...' : 'Качи снимка'}
                    <input type="file" accept="image/*" hidden onChange={handleImageUpload} />
                  </Button>
                  {!editing && <Button variant="outlined" onClick={() => setEditing(true)}>Редактирай</Button>}
                  <Button variant="outlined" color="error" onClick={() => setDeleteDialog(true)}>Изтрий</Button>
                </Box>
              </Box>
              {restaurant.imageUrl && (
                <Box component="img" src={restaurant.imageUrl} alt={restaurant.name}
                  sx={{ width: '100%', height: 200, objectFit: 'cover', borderRadius: 1, mb: 2 }} />
              )}
              <Divider sx={{ mb: 3 }} />
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                <TextField label="Име" name="name" variant="filled" fullWidth value={form.name} onChange={handleChange} disabled={!editing} />
                <TextField label="Описание" name="description" variant="filled" fullWidth multiline rows={3} value={form.description} onChange={handleChange} disabled={!editing} />
                <TextField label="Адрес" name="address" variant="filled" fullWidth value={form.address} onChange={handleChange} disabled={!editing} />
                <Box sx={{ display: 'flex', gap: 2 }}>
                  <TextField label="Град" name="city" variant="filled" fullWidth value={form.city} onChange={handleChange} disabled={!editing} />
                  <TextField label="Държава" name="country" variant="filled" fullWidth value={form.country} onChange={handleChange} disabled={!editing} />
                </Box>
                <Box sx={{ display: 'flex', gap: 2 }}>
                  <TextField label="Телефон" name="phone" variant="filled" fullWidth value={form.phone} onChange={handleChange} disabled={!editing} />
                  <TextField label="Имейл" name="email" variant="filled" fullWidth value={form.email} onChange={handleChange} disabled={!editing} />
                </Box>
                <Box sx={{ display: 'flex', gap: 2 }}>
                  <TextField label="Отваряне" name="openingTime" variant="filled" fullWidth value={form.openingTime} onChange={handleChange} disabled={!editing} />
                  <TextField label="Затваряне" name="closingTime" variant="filled" fullWidth value={form.closingTime} onChange={handleChange} disabled={!editing} />
                </Box>
              </Box>
              {editing && (
                <Box sx={{ display: 'flex', gap: 2, mt: 3 }}>
                  <Button variant="contained" onClick={handleSave} disabled={saving}>
                    {saving ? 'Запис...' : 'Запази'}
                  </Button>
                  <Button variant="outlined" onClick={() => { setEditing(false); setForm(toForm(restaurant)); }} disabled={saving}>
                    Отказ
                  </Button>
                </Box>
              )}
            </Paper>

            {/* Локации */}
            <Paper elevation={3} sx={{ p: 4 }}>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>Зони</Typography>
              <Divider sx={{ mb: 2 }} />
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                {locations.map(l => (
                  <Box key={l.location} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography>{LOCATION_LABELS[l.location] || l.location}</Typography>
                    <FormControlLabel
                      control={<Switch checked={l.enabled} onChange={() => handleToggleLocation(l.location, l.enabled)} />}
                      label={l.enabled ? 'Активна' : 'Неактивна'}
                      labelPlacement="start"
                    />
                  </Box>
                ))}
              </Box>
            </Paper>

            {/* Маси */}
            <Paper elevation={3} sx={{ p: 4 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6" sx={{ fontWeight: 700 }}>Маси ({tables.length})</Typography>
                <Button variant="outlined" startIcon={<AddIcon />} onClick={() => setAddingTable(true)}>
                  Добави маса
                </Button>
              </Box>
              <Divider sx={{ mb: 2 }} />
              {tables.length === 0 && <Typography color="text.secondary">Няма добавени маси.</Typography>}
              {tables.length > 0 && (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Номер</TableCell>
                      <TableCell>Капацитет</TableCell>
                      <TableCell>Зона</TableCell>
                      <TableCell>Статус</TableCell>
                      <TableCell align="right">Действие</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {tables.map(t => (
                      <TableRow key={t.id}>
                        <TableCell>{t.tableNumber}</TableCell>
                        <TableCell>{t.capacity} гости</TableCell>
                        <TableCell>{LOCATION_LABELS[t.location] || t.location}</TableCell>
                        <TableCell>
                          <Chip
                            label={t.available ? 'Активна' : 'Неактивна'}
                            color={t.available ? 'success' : 'default'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell align="right">
                          <Box sx={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center' }}>
                            <Switch checked={t.available} size="small"
                              onChange={() => handleToggleTable(t.tableNumber, t.available)}
                              title={t.available ? 'Деактивирай' : 'Активирай'} />
                            <IconButton size="small" onClick={() => {
                              setEditTableForm({ tableNumber: t.tableNumber, capacity: t.capacity, location: t.location });
                              setEditTableDialog({ open: true, table: t });
                            }}>
                              <EditIcon fontSize="small" />
                            </IconButton>
                            <IconButton size="small" color="error" onClick={() => setDeleteTableDialog({ open: true, table: t })}>
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Box>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </Paper>
          </Box>
        )}
      </Container>

      {/* Диалог за добавяне на маса */}
      <Dialog open={addingTable} onClose={() => setAddingTable(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Добави маса</DialogTitle>
        <DialogContent>
          {tableError && <Alert severity="error" sx={{ mb: 2 }}>{tableError}</Alert>}
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <TextField label="Номер на масата" variant="filled" fullWidth value={tableForm.tableNumber}
              onChange={e => setTableForm(f => ({ ...f, tableNumber: e.target.value }))} />
            <TextField label="Капацитет (гости)" type="number" variant="filled" fullWidth value={tableForm.capacity}
              onChange={e => setTableForm(f => ({ ...f, capacity: Number(e.target.value) }))}
              slotProps={{ htmlInput: { min: 1, max: 20 } }} />
            <TextField label="Зона" variant="filled" fullWidth select value={tableForm.location}
              onChange={e => setTableForm(f => ({ ...f, location: e.target.value }))}>
              {Object.entries(LOCATION_LABELS).map(([val, label]) => (
                <MenuItem key={val} value={val}>{label}</MenuItem>
              ))}
            </TextField>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setAddingTable(false); setTableError(''); }}>Отказ</Button>
          <Button variant="contained" onClick={handleAddTable} disabled={!tableForm.tableNumber || !tableForm.capacity}>
            Добави
          </Button>
        </DialogActions>
      </Dialog>

      {/* Диалог за изтриване */}
      <Dialog open={deleteDialog} onClose={() => setDeleteDialog(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Изтриване на ресторант</DialogTitle>
        <DialogContent>
          <Typography>
            Сигурни ли сте, че искате да изтриете <strong>{restaurant?.name}</strong>? Това действие не може да се отмени.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialog(false)}>Отказ</Button>
          <Button variant="contained" color="error" onClick={handleDelete} disabled={deleting}>
            {deleting ? 'Изтриване...' : 'Изтрий'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Диалог за редактиране на маса */}
      <Dialog open={editTableDialog.open} onClose={() => setEditTableDialog({ open: false, table: null })} maxWidth="xs" fullWidth>
        <DialogTitle>Редактирай маса {editTableDialog.table?.tableNumber}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <TextField label="Капацитет (гости)" type="number" variant="filled" fullWidth
              value={editTableForm.capacity}
              onChange={e => setEditTableForm(f => ({ ...f, capacity: Number(e.target.value) }))}
              slotProps={{ htmlInput: { min: 1, max: 20 } }} />
            <TextField label="Зона" variant="filled" fullWidth select
              value={editTableForm.location}
              onChange={e => setEditTableForm(f => ({ ...f, location: e.target.value }))}>
              {Object.entries(LOCATION_LABELS).map(([val, label]) => (
                <MenuItem key={val} value={val}>{label}</MenuItem>
              ))}
            </TextField>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditTableDialog({ open: false, table: null })}>Отказ</Button>
          <Button variant="contained" onClick={handleEditTable}>Запази</Button>
        </DialogActions>
      </Dialog>

      {/* Диалог за изтриване на маса */}
      <Dialog open={deleteTableDialog.open} onClose={() => setDeleteTableDialog({ open: false, table: null })} maxWidth="xs" fullWidth>
        <DialogTitle>Изтриване на маса</DialogTitle>
        <DialogContent>
          <Typography>Сигурни ли сте, че искате да изтриете маса <strong>{deleteTableDialog.table?.tableNumber}</strong>?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTableDialog({ open: false, table: null })}>Отказ</Button>
          <Button variant="contained" color="error" onClick={handleDeleteTable}>Изтрий</Button>
        </DialogActions>
      </Dialog>

      {/* Диалог за нов ресторант */}
      <Dialog open={createDialog} onClose={() => setCreateDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Нов ресторант</DialogTitle>
        <DialogContent>
          {createError && <Alert severity="error" sx={{ mb: 2 }}>{createError}</Alert>}
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
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
              <TextField label="Отваряне" variant="filled" fullWidth value={createForm.openingTime} onChange={e => setCreateForm(f => ({ ...f, openingTime: e.target.value }))} placeholder="09:00" />
              <TextField label="Затваряне" variant="filled" fullWidth value={createForm.closingTime} onChange={e => setCreateForm(f => ({ ...f, closingTime: e.target.value }))} placeholder="22:00" />
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
          <Button onClick={() => { setCreateDialog(false); setCreateForm(emptyCreateForm); setCreateError(''); }}>Отказ</Button>
          <Button variant="contained" onClick={handleCreate} disabled={creating || !createForm.name || !createForm.address}>
            {creating ? 'Създаване...' : 'Създай'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default AdminRestaurantPage;
