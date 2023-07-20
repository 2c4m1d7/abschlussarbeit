import { securedFetch, handleResponse } from "../../SecuredFetch"
import { getUserSuccess, loginSuccess, logout, requestUser } from "../slices/loginSlice"
import secureApi from "../../api/secureApi";

export const fetchUser =
  () => (dispatch, getState) => {

    dispatch(requestUser())

    return secureApi.get("user")
      .then(response => {
        dispatch(getUserSuccess(response.data))
        dispatch(loginSuccess())
      })
      .catch(error => {

        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        dispatch(logout())
        return Promise.reject(error)

      })
    //  return securedFetch("http://127.0.0.1:9000/user")
    //      .then(handleResponse(dispatch))
    //      .then(user => dispatch(getUserSuccess(user)))
  }
