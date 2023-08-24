import axios from 'axios';

const API_BASE_URL = process.env.NODE_ENV === 'production' ? '/api' : '';

const unsecuredApi = axios.create({
    baseURL: API_BASE_URL,
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json',
    },
});

export default unsecuredApi;