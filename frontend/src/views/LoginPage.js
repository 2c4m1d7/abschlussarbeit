import React, { useState, useEffect } from 'react';
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { login } from "../redux/thunks/loginThunks";
import { fetchUser } from '../redux/thunks/userThunks';



const LoginPage = (props) => {
  const dispatch = useDispatch();
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
    dispatch(login(payload))
      .then(x => dispatch(fetchUser))
  }


  return (
    <div className="min-h-screen flex items-center justify-center bg-blue-400">
      <div className="bg-white p-16 rounded-lg shadow-2xl max-w-lg mx-auto ">
        <h2 className="text-3xl font-bold mb-10 text-gray-800">Login</h2>
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="flex flex-col">
            <label htmlFor="username" className="mb-2 text-sm text-gray-600 dark:text-gray-400">Username:</label>
            <input type="text" id="username" value={username} required
              onChange={(e) => setUsername(e.target.value)} className="px-3 py-2 placeholder-gray-300 border rounded-md focus:outline-none focus:ring focus:ring-indigo-100 focus:border-indigo-300 dark:bg-gray-700 dark:text-white dark:placeholder-gray-500 dark:border-gray-600 dark:focus:ring-gray-900 dark:focus:border-gray-500" />
          </div>
          <div className="flex flex-col">
            <label htmlFor="password" className="mb-2 text-sm text-gray-600 dark:text-gray-400">Password:</label>
            <input type="password" id="password" value={password} required
              onChange={(e) => setPassword(e.target.value)} className="px-3 py-2 placeholder-gray-300 border rounded-md focus:outline-none focus:ring focus:ring-indigo-100 focus:border-indigo-300 dark:bg-gray-700 dark:text-white dark:placeholder-gray-500 dark:border-gray-600 dark:focus:ring-gray-900 dark:focus:border-gray-500" />
          </div>
          <button type="submit" className="w-full px-3 py-2 text-white bg-indigo-500 rounded-md focus:bg-indigo-600 focus:outline-none">
            Login</button>
        </form>
      </div>
    </div>




  );
}

export default LoginPage;
