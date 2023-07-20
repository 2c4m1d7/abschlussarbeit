import React, { useEffect, useState } from 'react';

import { BrowserRouter as Router, Route, Routes, Navigate, } from "react-router-dom";

import LoginPage from './views/LoginPage';
import Databases from './views/Databases';
import DatabaseOverview from './views/DatabaseOverview';
import { useDispatch, useSelector } from "react-redux";
import { loginSuccess } from "./redux/slices/loginSlice";
import { fetchUser } from './redux/thunks/userThunks';

const App = () => {
  const { isLoggedIn } = useSelector(state => state.login);
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(fetchUser())
      .catch(error => console.log(error))
  }, [dispatch]);

  return (
    <Router>
      <Routes>
        <Route path="/" element={isLoggedIn ? <Navigate to="/overview" /> : <Navigate to="/login" />} />
        <Route path="/login" element={isLoggedIn ? <Navigate to="/overview" /> : <LoginPage />} />
        <Route path="/overview" element={isLoggedIn ? <DatabaseOverview /> : <Navigate to="/login" />} />
      </Routes>
    </Router>
  );

}

export default App;