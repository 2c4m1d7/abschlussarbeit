import React, { useState } from 'react';
import { useNavigate } from "react-router-dom";




const LoginPage = (props) => {
  const [username, setUsername] = useState('user01');
  const [password, setPassword] = useState('password1');
  const navigate = useNavigate();
  const handleSubmit = (event) => {
    event.preventDefault();
    if (username === '' || password === '') {
      return;
    }

    const payload = {
      username: username,
      password: password
    }

    //  fetch("http://127.0.0.1:9000/v1/auth/signin", {
    //   method: "POST",
    //   headers: {
    //     "Content-Type": "application/json",
    //   },
    //   body: JSON.stringify(payload),
    // }).then(response => {
    //   console.log(response)
    //   return response.json()
    // }).then(data => {
    //   localStorage.setItem('accsessToken', data.accessToken)
    //   localStorage.setItem('refreshToken', data.refreshToken)
    //   console.log(props)
    //   props.setIsLoggedIn(true)
    //   navigate("/overview");
    // })

    fetch("http://127.0.0.1:9000/", {
      method: "POST",
      credentials: 'same-origin',
      headers: {
        Accept: 'application/json',
        "Content-Type": "application/json",
        
      },
      body: JSON.stringify(payload),
    })
    .then(response => {
      return response.json()
    }).then(data => {
      console.log(data)
      // localStorage.setItem('accsessToken', data.accessToken)
      // localStorage.setItem('refreshToken', data.refreshToken)
      // console.log(props)
      // props.setIsLoggedIn(true)
      // navigate("/overview");
    }).catch(error => {
      console.log(error)
    })

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
