// secureApi.js
import unsecuredApi from './unsecuredApi';
import axios from 'axios';

const secureApi = axios.create({
    baseURL: 'http://localhost:9000',
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json',
    },
});

secureApi.interceptors.request.use(
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

secureApi.interceptors.response.use((response) => {
    return response
}, async function (error) {
    const originalRequest = error.config;
    if (error.response.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;
        const refreshToken = localStorage.getItem('refreshToken');
        try {
            const accessToken = (await unsecuredApi.post('token/refresh', { refreshToken: refreshToken })).data
            localStorage.setItem('accessToken', accessToken);
            return secureApi(originalRequest);
        } catch (ex) {
            return Promise.reject(ex);
        }
    }
    return Promise.reject(error);
});

export default secureApi;
