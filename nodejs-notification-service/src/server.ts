import 'dotenv/config';
import express from 'express';
import { initDb } from './db/database';
import notificationsRouter from './routes/notifications';

const app = express();
app.use(express.json());

app.use('/api/notifications', notificationsRouter);

const PORT = parseInt(process.env.PORT || '3001');

initDb()
    .then(() => {
        app.listen(PORT, () => {
            console.log(`Notification service running on port ${PORT}`);
        });
    })
    .catch((err: Error) => {
        console.error('Failed to initialize database:', err);
        process.exit(1);
    });
