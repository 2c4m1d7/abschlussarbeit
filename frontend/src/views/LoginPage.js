import React, { useState, useEffect } from 'react';
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { login } from "../redux/thunks/loginThunks";
import { fetchUser } from '../redux/thunks/userThunks';


const LoginPage = (props) => {
  const dispatch = useDispatch();
  const state = useSelector(state => state);
  const [username, setUsername] = useState('user01');
  const [password, setPassword] = useState('password1');
  const navigate = useNavigate();

  // useEffect(() => {
  //   if (state.login.isLoggedIn === true) {
  //     navigate("/overview");
  //   }f
  // }, [state]);
  const handleSubmit = (event) => {
    console.log(state)

    event.preventDefault();
    if (username === '' || password === '') {
      return;
    }

    const payload = {
      username: username,
      password: password
    }
    dispatch(login(payload))
    .then(x => dispatch(fetchUser))
  }

  return (
    <div>
      <h2>Login</h2>
      <form onSubmit={handleSubmit}>
        <label htmlFor="username">Username:</label>
        <input type="text" id="username" value={username} required
          onChange={(e) => setUsername(e.target.value)} />
        <label htmlFor="password">Password:</label>
        <input type="password" id="password" value={password} required
          onChange={(e) => setPassword(e.target.value)} />
        <button type="submit">Submit</button>
      </form>
    </div>
  );
}

export default LoginPage;
