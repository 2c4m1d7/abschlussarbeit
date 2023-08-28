import { combineReducers } from '@reduxjs/toolkit';
import  sessionReducer  from './sessionSlice';

export default combineReducers({
  session: sessionReducer,

});