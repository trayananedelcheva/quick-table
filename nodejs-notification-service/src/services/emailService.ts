import nodemailer from 'nodemailer';

export interface NotificationRequest {
    type: string;
    recipientEmail: string;
    recipientName: string;
    reservationId: number;
    restaurantName: string;
    reservationDate: string;
    reservationTime: string;
    numberOfGuests: number;
    specialRequests?: string;
    userId?: number;
}

const transporter = nodemailer.createTransport({
    host: process.env.SMTP_HOST || 'smtp.gmail.com',
    port: parseInt(process.env.SMTP_PORT || '465'),
    secure: true,
    auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASSWORD,
    },
});

function getSubject(type: string, restaurantName: string): string {
    switch (type) {
        case 'RESERVATION_CONFIRMED': return `Потвърдена резервация - ${restaurantName}`;
        case 'RESERVATION_CANCELLED': return `Отказана резервация - ${restaurantName}`;
        case 'RESERVATION_REMINDER': return `Напомняне за резервация - ${restaurantName}`;
        default: return `Нотификация от Quick Table - ${restaurantName}`;
    }
}

function buildHtml(req: NotificationRequest): string {
    return `
        <h2>Здравейте, ${req.recipientName}!</h2>
        <p>Вашата резервация е потвърдена.</p>
        <table>
            <tr><td><strong>Ресторант:</strong></td><td>${req.restaurantName}</td></tr>
            <tr><td><strong>Дата:</strong></td><td>${req.reservationDate}</td></tr>
            <tr><td><strong>Час:</strong></td><td>${req.reservationTime}</td></tr>
            <tr><td><strong>Гости:</strong></td><td>${req.numberOfGuests}</td></tr>
            ${req.specialRequests ? `<tr><td><strong>Специални изисквания:</strong></td><td>${req.specialRequests}</td></tr>` : ''}
        </table>
        <p>Благодарим ви, че избрахте Quick Table!</p>
    `;
}

export async function sendEmail(data: NotificationRequest): Promise<void> {
    await transporter.sendMail({
        from: `"Quick Table" <${process.env.SMTP_USER}>`,
        to: data.recipientEmail,
        subject: getSubject(data.type, data.restaurantName),
        html: buildHtml(data),
    });
}
