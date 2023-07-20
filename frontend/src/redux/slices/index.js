import { combineReducers } from '@reduxjs/toolkit';
import  loginReducer  from './loginSlice';

export default combineReducers({
  login: loginReducer,

});