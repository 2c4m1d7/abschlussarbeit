import React, { useEffect, useState } from 'react';

import { BrowserRouter as Router, Route, Routes, Navigate, } from "react-router-dom";

import LoginPage from './views/LoginPage';
import MainPage from './views/MainPage';
import { useDispatch, useSelector } from "react-redux";
import { loginSuccess } from "./redux/slices/sessionSlice";
import { fetchUser } from './redux/thunks/userThunks';
import DatabaseDetails from './views/DatabaseDetails';

const App = () => {
  const { isLoggedIn, loading, error } = useSelector(state => state.session);
  const dispatch = useDispatch();

  useEffect(() => {

    dispatch(fetchUser())
      .then(dispatch(loginSuccess()))
      .catch(error => {
        console.log(error)
      })
  }, [dispatch]);

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <Router>
      <Routes>
        <Route path="/" element={isLoggedIn ? <Navigate to="/overview" /> : <Navigate to="/login" />} />
        <Route path="/login" element={isLoggedIn ? <Navigate to="/overview" /> : <LoginPage />} />
        <Route path="/overview" element={isLoggedIn ? <MainPage /> : <Navigate to="/login" />} />
        <Route path="/database/:id" element={isLoggedIn ? <DatabaseDetails /> : <Navigate to="/login" />} />
      </Routes>
    </Router>
  );

}

export default App;