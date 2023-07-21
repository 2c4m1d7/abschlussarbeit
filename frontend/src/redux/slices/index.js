import { combineReducers } from '@reduxjs/toolkit';
import  loginReducer  from './userSlice';

export default combineReducers({
  login: loginReducer,

});