import { Router, Request, Response } from 'express';
import { sendEmail, NotificationRequest } from '../services/emailService';
import { pool } from '../db/database';

const router = Router();

router.post('/send', async (req: Request, res: Response) => {
    const data: NotificationRequest = req.body;
    let success = false;
    let errorMessage: string | null = null;

    try {
        await sendEmail(data);
        success = true;
    } catch (err) {
        errorMessage = (err as Error).message;
        console.error('Грешка при изпращане на email:', errorMessage);
    }

    const result = await pool.query(
        `INSERT INTO notifications
            (type, recipient_email, recipient_name, reservation_id, restaurant_name,
             reservation_date, reservation_time, number_of_guests, special_requests, user_id, success, error_message)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12) RETURNING id`,
        [data.type, data.recipientEmail, data.recipientName, data.reservationId,
         data.restaurantName, data.reservationDate, data.reservationTime,
         data.numberOfGuests, data.specialRequests ?? null, data.userId ?? null, success, errorMessage]
    );

    res.json({ success, id: result.rows[0].id });
});

router.get('/history', async (_req: Request, res: Response) => {
    const result = await pool.query(
        'SELECT * FROM notifications ORDER BY sent_at DESC LIMIT 100'
    );
    res.json(result.rows);
});

router.get('/history/:userId', async (req: Request, res: Response) => {
    const result = await pool.query(
        'SELECT * FROM notifications WHERE user_id = $1 ORDER BY sent_at DESC',
        [req.params.userId]
    );
    res.json(result.rows);
});

export default router;
