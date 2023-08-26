
import unsecuredApi from './unsecuredApi';
import axios from 'axios';

const API_BASE_URL = process.env.NODE_ENV === 'production' ? '/api' : '';

const securedApi = axios.create({
    baseURL: API_BASE_URL,
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json',
    },
});

securedApi.interceptors.request.use(
    config => {
        const accessToken = localStorage.getItem('accessToken');
        if (accessToken) {
            config.headers['Authorization'] = `Bearer ${accessToken}`;
        }
        return config;
    },
    error => {
        Promise.reject(error)
    });

securedApi.interceptors.response.use((response) => {
    return response
}, async function (error) {
    const originalRequest = error.config;
    if (error.response.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;
        const refreshToken = localStorage.getItem('refreshToken');
        try {
            const accessToken = (await unsecuredApi.post('token/refresh', { refreshToken: refreshToken })).data
            localStorage.setItem('accessToken', accessToken);
            return securedApi(originalRequest);
        } catch (ex) {
            return Promise.reject(ex);
        }
    }
    return Promise.reject(error);
});

export default securedApi;
