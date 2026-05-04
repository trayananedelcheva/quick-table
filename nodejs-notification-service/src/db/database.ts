import { Pool } from 'pg';

const pool = new Pool({
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5432'),
    database: process.env.DB_NAME || 'quicktable_notifications_node',
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD,
});

export async function initDb(): Promise<void> {
    await pool.query(`
        CREATE TABLE IF NOT EXISTS notifications (
            id SERIAL PRIMARY KEY,
            type VARCHAR(100) NOT NULL,
            recipient_email VARCHAR(255) NOT NULL,
            recipient_name VARCHAR(255) NOT NULL,
            reservation_id BIGINT NOT NULL,
            restaurant_name VARCHAR(255) NOT NULL,
            reservation_date VARCHAR(20) NOT NULL,
            reservation_time VARCHAR(10) NOT NULL,
            number_of_guests INT NOT NULL,
            special_requests TEXT,
            user_id BIGINT,
            sent_at TIMESTAMP DEFAULT NOW(),
            success BOOLEAN NOT NULL,
            error_message TEXT
        )
    `);
}

export { pool };
