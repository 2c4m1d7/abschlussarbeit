import { loginSuccess, requestLogin, logout, loginFailed } from "../slices/sessionSlice"
import jwt_decode from "jwt-decode";
import { fetchUser } from "./userThunks";
import unsecuredApi from "../../api/unsecuredApi";
import securedApi from "../../api/securedApi";


export const login =
    (credentials) => async (dispatch, getState) => {

        dispatch(requestLogin())
        unsecuredApi.post('signin', credentials)
            .then(response => {
                localStorage.setItem('accessToken', response.data.accessToken)
                localStorage.setItem('refreshToken', response.data.refreshToken)
                dispatch(fetchUser())
                    .then(dispatch(loginSuccess()))
                    .catch(error => console.log(error))
                return
            })
            .catch(error => {
                dispatch(loginFailed(error))
                return
            })

    }


export const refreshAccessToken =
    () => async (dispatch, getState) => {

        dispatch(requestLogin())

        let refreshToken = localStorage.getItem('refreshToken')
        if (refreshToken === null) {
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

        const newAccessToken = (await unsecuredApi.post('token/refresh', { refreshToken: refreshToken })).data
        localStorage.setItem('accessToken', newAccessToken)
        dispatch(fetchUser())
            // .then(x => dispatch(loginSuccess()))
            .catch(error => console.log(error))

    }


