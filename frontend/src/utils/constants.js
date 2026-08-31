export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'saas_access_token',
  REFRESH_TOKEN: 'saas_refresh_token',
  USER_DATA: 'saas_user_data',
};

export const PLANS = {
  FREE: {
    code: 'FREE',
    name: 'Free Plan',
    price: 0,
    currency: '₹',
    interval: 'month',
    aiLimit: 50,
    storageMb: 100,
  },
  PRO: {
    code: 'PRO',
    name: 'Pro Plan',
    price: 499,
    yearlyPrice: 4990,
    currency: '₹',
    interval: 'month',
    aiLimit: 1000,
    storageMb: 5120,
  },
  BUSINESS: {
    code: 'BUSINESS',
    name: 'Business Plan',
    price: 1499,
    yearlyPrice: 14990,
    currency: '₹',
    interval: 'month',
    aiLimit: 5000,
    storageMb: 51200,
  },
};
