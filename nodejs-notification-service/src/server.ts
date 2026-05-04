import 'dotenv/config';
import express, { Request, Response, NextFunction } from 'express';
import morgan from 'morgan';
import { initDb } from './db/database';
import notificationsRouter from './routes/notifications';

const app = express();
app.use(express.json());
app.use(morgan('dev'));

app.use('/api/notifications', notificationsRouter);

app.use((err: Error, req: Request, res: Response, _next: NextFunction) => {
    console.error(`[ERROR] ${req.method} ${req.path} — ${err.message}`);
    console.error(err.stack);
    res.status(500).json({ error: err.message });
});

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
