import React, { useEffect, useState } from 'react';

import {
  BrowserRouter as Router,
  Route,
  Routes,
  Navigate,
} from "react-router-dom";

import LoginPage from './views/LoginPage';
import Databases from './views/Databases';


const App = () => {
  const [isLoggedIn, setIsLoggedIn] = useState(checkLoggedIn());

console.log(isLoggedIn)
  return (
    <Router>
      <Routes>
        <Route path="/" element={isLoggedIn ? <Navigate to="/overview" /> : <Navigate to="/login" />} />
        <Route
          path="/login"
          element={isLoggedIn ? <Navigate to="/overview" /> : <LoginPage setIsLoggedIn={setIsLoggedIn}/>}
        />
        <Route
          path="/overview"
          element={isLoggedIn ? <Databases /> : <Navigate to="/login" />}
        />
      </Routes>
    </Router>
  );


}

const checkLoggedIn = () => {
  let accsessToken = localStorage.getItem('accsessToken')
  let refreshToken = localStorage.getItem('refreshToken')

  if (accsessToken === null && refreshToken === null) {
    return false
  }
  return fetch("http://127.0.0.1:9000/v1/user", {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: 'Bearer ' + accsessToken
    }
  }).then(response => {
    return response.json()
  }).then(data => {
    if (data.status === 200) {
      return true
    }
    return false
  }).catch(error => {
    console.log(error)
    localStorage.removeItem('accsessToken')
    localStorage.removeItem('refreshToken')
    return false
  })

}
export default App;
