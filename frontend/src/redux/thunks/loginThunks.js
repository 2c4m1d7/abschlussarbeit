import { loginSuccess, requestLogin, logout } from "../slices/loginSlice"
import Client from "../../api/Client"
import jwt_decode from "jwt-decode";
import { fetchUser } from "./userThunks";
import unsecuredApi from "../../api/unsecuredApi";
import secureApi from "../../api/secureApi";


export const login =
    (credentials) => async (dispatch, getState) => {

        dispatch(requestLogin())
        const tokens = (await unsecuredApi.post('signin', credentials)).data
        localStorage.setItem('accessToken', tokens.accessToken)
        localStorage.setItem('refreshToken', tokens.refreshToken)

        dispatch(fetchUser())
        .catch(error => console.log(error))

    }


export const refreshAccessToken =
    () => async (dispatch, getState) => {

        dispatch(requestLogin())
        let accessToken = localStorage.getItem('accessToken')
        let refreshToken = localStorage.getItem('refreshToken')
        if (accessToken === null && refreshToken === null) {
            dispatch(logout())
            return
        }

        const decodedRefreshToken = jwt_decode(refreshToken)
        const currentTimestampInSeconds = Math.floor(Date.now() / 1000);
        if (decodedRefreshToken.exp < currentTimestampInSeconds) {
            localStorage.removeItem('accessToken')
            localStorage.removeItem('refreshToken')
            dispatch(logout())
            return
        }

        const newAccessToken =  (await unsecuredApi.post('token/refresh', { refreshToken: refreshToken })).data
        localStorage.setItem('accessToken', newAccessToken)
        dispatch(fetchUser())
        // .then(x => dispatch(loginSuccess()))
        .catch(error => console.log(error))

    }


