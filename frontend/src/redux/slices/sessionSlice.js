
import { createSlice } from '@reduxjs/toolkit'

const sessionSlice = createSlice({
  name: 'session',
  initialState: {
    isLoggedIn: false,
    loading: true,
    error: null,
    user: null
  },
  reducers: {
    requestLogin: (state) => {
      state.loading = true;
    },
    loginSuccess: (state, action) => {
      state.isLoggedIn = true;
      state.loading = false;
    },
    loginFailed: (state, action) => {
      state.error = action.payload;
      state.loading = false;
    },
    logout: (state) => {
      state.isLoggedIn = false;
      state.user = null;
    },
    requestUser: (state) => {
      state.loading = true;
    },
    getUserSuccess: (state, action) => {
      state.user = action.payload;
      state.loading = false;
    },
    getUserFailed: (state, action) => {
      state.error = action.payload;
      state.loading = false;
    },
  }
});

export const { requestLogin, loginSuccess, loginFailed, logout, requestUser, getUserSuccess, getUserFailed } = sessionSlice.actions;

export default sessionSlice.reducer;