// import { createSlice } from '@reduxjs/toolkit'


// const initialState = {
//   isLoggedIn: undefined,
//   loading: false,
//   error: null
// }



// export const loginSlice = createSlice({
//   name: 'loginSlice',
//   initialState: initialState,
//   reducers: {
//     loginStart(state, action) {
//       state.loading = true;
//       state.error = undefined;
//       state.isLoggedIn = undefined;
//     },
//     loginSuccess(state, action) {
//       state.isLoggedIn = true;
//       state.error = undefined;
//       state.loading = false;
//     },
//     loginFailed(state, action) {
//       state.error = action.payload;
//       state.loading = false;
//     },
//     restoreSession(state) {
//       state.isLoggedIn = undefined;
//     },
//     logout(state) {
//       state.isLoggedIn = false;
//       state.error = undefined;
//       state.loading = false;
//     },
//   },
// })

// // Action creators are generated for each case reducer function
// export const {
//   loginStart,
//   loginSuccess,
//   loginFailed,
//   restoreSession,
//   logout,
// } = loginSlice.actions

// export default loginSlice.reducer



import { createSlice } from '@reduxjs/toolkit'

const userSlice = createSlice({
  name: 'user',
  initialState: {
    isLoggedIn: false,
    loading: false,
    error: null,
    user: null
  },
  reducers: {
    requestLogin: (state) => {
      state.loading = true;
    },
    loginSuccess: (state, action) => {
      state.user = action.payload;
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

export const { requestLogin, loginSuccess, loginFailed, logout, requestUser, getUserSuccess, getUserFailed } = userSlice.actions;

export default userSlice.reducer;