import React from 'react';

interface LogoProps {
  size?: number;
  color?: string;
}

const QuickTableLogo: React.FC<LogoProps> = ({ size = 36, color = '#ffffff' }) => (
  <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* Чиния / циферблат */}
    <circle cx="20" cy="20" r="17" stroke={color} strokeWidth="2" fill="none" opacity="0.9" />
    <circle cx="20" cy="20" r="13" stroke={color} strokeWidth="1" fill="none" opacity="0.3" />

    {/* Вилица — лява стрелка (часова, сочи нагоре-ляво) */}
    <g transform="rotate(-40, 20, 20)">
      {/* Дръжка */}
      <rect x="19" y="20" width="2" height="9" rx="1" fill={color} />
      {/* Зъбци */}
      <rect x="17" y="9" width="1.2" height="6" rx="0.6" fill={color} />
      <rect x="19.4" y="9" width="1.2" height="6" rx="0.6" fill={color} />
      <rect x="21.8" y="9" width="1.2" height="6" rx="0.6" fill={color} />
      {/* Шийка на вилицата */}
      <rect x="19" y="14.5" width="2" height="5.5" rx="0.5" fill={color} />
    </g>

    {/* Нож — дясна стрелка (минутна, сочи надясно) */}
    <g transform="rotate(80, 20, 20)">
      {/* Дръжка */}
      <rect x="19" y="20" width="2" height="9" rx="1" fill={color} />
      {/* Острие */}
      <path d="M19 9 Q22 11 21 19 L19 19 Z" fill={color} opacity="0.9" />
    </g>

    {/* Централна точка */}
    <circle cx="20" cy="20" r="2" fill={color} />
  </svg>
);

export default QuickTableLogo;
