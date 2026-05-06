import nodemailer from 'nodemailer';
import Handlebars from 'handlebars';
import fs from 'node:fs';
import path from 'node:path';

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
    rejectionReason?: string;
    reviewUrl?: string;
}

const transporter = nodemailer.createTransport({
    host: process.env.SMTP_HOST || 'smtp.gmail.com',
    port: Number.parseInt(process.env.SMTP_PORT || '465'),
    secure: true,
    auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASSWORD,
    },
});

const TEMPLATES_DIR = path.join(__dirname, '../templates/email');

const templateCache: Record<string, HandlebarsTemplateDelegate> = {};

function loadTemplate(name: string): HandlebarsTemplateDelegate {
    if (!templateCache[name]) {
        const filePath = path.join(TEMPLATES_DIR, `${name}.hbs`);
        const source = fs.readFileSync(filePath, 'utf-8');
        templateCache[name] = Handlebars.compile(source);
    }
    return templateCache[name];
}

const TYPE_TO_TEMPLATE: Record<string, string> = {
    RESERVATION_CONFIRMED: 'reservation-confirmed',
    RESERVATION_CANCELLED: 'reservation-cancelled',
    RESERVATION_REJECTED: 'reservation-rejected',
    RESERVATION_COMPLETED: 'reservation-completed',
    RESERVATION_NO_SHOW: 'reservation-no-show',
};

const TYPE_TO_SUBJECT: Record<string, (restaurantName: string) => string> = {
    RESERVATION_CONFIRMED: (r) => `Потвърдена резервация - ${r}`,
    RESERVATION_CANCELLED: (r) => `Отказана резервация - ${r}`,
    RESERVATION_REJECTED: (r) => `Резервацията ви беше отхвърлена - ${r}`,
    RESERVATION_NO_SHOW:  (r) => `Пропусната резервация - ${r}`,
    RESERVATION_COMPLETED: (r) => `Благодарим ви! Оставете ревю за ${r}`,
};

export async function sendEmail(data: NotificationRequest): Promise<void> {
    const templateName = TYPE_TO_TEMPLATE[data.type];
    if (!templateName) {
        throw new Error(`Няма шаблон за тип: ${data.type}`);
    }

    const template = loadTemplate(templateName);
    const html = template(data);

    const subjectFn = TYPE_TO_SUBJECT[data.type];
    const subject = subjectFn ? subjectFn(data.restaurantName) : `Нотификация от Quick Table - ${data.restaurantName}`;

    await transporter.sendMail({
        from: `"Quick Table" <${process.env.SMTP_USER}>`,
        to: data.recipientEmail,
        subject,
        html,
    });
}
