import { refreshAccessToken } from "./redux/thunks/loginThunks";

function updateOptions(options) {
    const update = { ...options };
    const token = localStorage.getItem('accessToken')
    if (token) {
        update.headers = {
            ...update.headers,
            Authorization: `Bearer ${token}`,
        };
    }
    return update;
}



export function securedFetch(url, options) {
    return fetch(url, updateOptions(options))
}


export function handleResponse(dispatch) {
    return response => {
        console.log(response)
        if (response.status === 401) {
            dispatch(refreshAccessToken())
            return null
        }

        return response.json();
    };

}



