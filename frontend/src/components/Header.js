import React from 'react';
import { logout } from '../redux/slices/userSlice';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from "react-redux";
import { AiOutlineUser } from 'react-icons/ai';
import { MdLogout } from 'react-icons/md';


const Header = ({ handleOpenAccountModal }) => {


    const dispatch = useDispatch();
    const { loading, user } = useSelector(state => state.login);


    return (
        <header className="flex items-center justify-end w-full fixed top-0 px-10 py-7">
            <AiOutlineUser size={30} onClick={handleOpenAccountModal} className='cursor-pointer text-blue-600 mr-4' />
            <MdLogout size={30} onClick={() => {
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                dispatch(logout())
            }} className='cursor-pointer text-blue-600' />
        </header>
    );
}

export default Header;