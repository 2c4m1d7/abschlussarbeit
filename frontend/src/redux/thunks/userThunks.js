
import { getUserSuccess, loginSuccess, logout, requestUser } from "../slices/sessionSlice"
import securedApi from "../../api/securedApi";

export const fetchUser =
  () => (dispatch, getState) => {

    dispatch(requestUser())

    return securedApi.get("user")
      .then(response => {
        dispatch(getUserSuccess(response.data))
      })
      .catch(error => {
        dispatch(logout())
        return Promise.reject(error)
      })
  }
