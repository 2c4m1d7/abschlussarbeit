import axios from 'axios';

const unsecuredApi = axios.create({
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json',
    },
});

export default unsecuredApi;