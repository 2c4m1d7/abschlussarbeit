
import { getUserSuccess, loginSuccess, logout, requestUser } from "../slices/userSlice"
import secureApi from "../../api/secureApi";

export const fetchUser =
  () => (dispatch, getState) => {

    dispatch(requestUser())

    return secureApi.get("user")
      .then(response => {
        dispatch(getUserSuccess(response.data))
      })
      .catch(error => {
        dispatch(logout())
        return Promise.reject(error)
      })
  }
