import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from "react-redux";
import { useParams } from 'react-router-dom';
import secureApi from '../api/secureApi';
import { logout } from '../redux/slices/userSlice';


const DatabaseDetails = () => {
    const dispatch = useDispatch();
    const { id } = useParams(); // get id from route
    const [db, setDb] = useState()// implement this yourself

    useEffect(() => {
        secureApi.get(`database/${id}`)
            .then(response => {
                setDb(response.data)
            })
            .catch(error => {
                console.log(error)
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                dispatch(logout())
            })
    }, [])

    return (
        <div>db details</div>
    )
};

export default DatabaseDetails;