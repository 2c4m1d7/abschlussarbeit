import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from "react-redux";
import { useParams } from 'react-router-dom';
import securedApi from '../api/securedApi';
import { loginSuccess, logout, requestUser } from '../redux/slices/sessionSlice';
import Header from '../components/Header';
import { useNavigate } from 'react-router-dom';
import { AiOutlineClose } from 'react-icons/ai';
import AccountModal from '../components/AccountModal';



const {
    REACT_APP_REDIS_HOSTNAME
  } = process.env;

const DatabaseDetails = () => {
    const dispatch = useDispatch();
    const { id } = useParams();
    const [details, setDetails] = useState({name:"...", port:"...", createdAt:"..."});

    const [isAccountModalOpen, setIsAccountModalOpen] = useState(false); 


    const handleOpenAccountModal = () => setIsAccountModalOpen(true); 
    const handleCloseAccountModal = () => setIsAccountModalOpen(false); 

    const navigate = useNavigate();

    const handleBack = () => {
        navigate(-1)
    };

    useEffect(() => {
        securedApi.get(`/database/${id}`)
            .then(response => {
                setDetails(response.data)
            })
            .catch(error => {
                console.log(error)
                navigate("/overview")
            })
    }, [id])

    return (
        <div className="h-screen bg-gray-100 flex items-center justify-center">
            <Header handleOpenAccountModal={handleOpenAccountModal} />
            {isAccountModalOpen && (
                <AccountModal handleCloseModal={handleCloseAccountModal} />
            )}
            <div className="bg-white shadow overflow-hidden sm:rounded-lg max-w-xl mx-auto mt-10">
                <div className="flex justify-between items-center px-4 py-5 sm:px-6">
                    <h3 className="text-lg leading-6 font-medium text-gray-900">Database Details</h3>
                    <button
                        onClick={handleBack}
                        className="bg-gray-300 hover:bg-gray-400 text-gray-800 font-bold py-1 px-2 rounded inline-flex items-center"
                    >
                        <AiOutlineClose size={18} />
                    </button>
                </div>
                <div className="border-t border-gray-200">
                    <dl>
                        <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                            <dt className="text-sm font-medium text-gray-500">Name</dt>
                            <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{details.name}</dd>
                        </div>
                        <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                            <dt className="text-sm font-medium text-gray-500">Port</dt>
                            <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{details.port}</dd>
                        </div>
                        <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                            <dt className="text-sm font-medium text-gray-500">Created At</dt>
                            <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{new Date(details.createdAt).toLocaleString()}</dd>
                        </div>
                        <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                            <dt className="text-sm font-medium text-gray-500">Connect</dt>
                            <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">redis-cli -h {REACT_APP_REDIS_HOSTNAME} -p {details.port}</dd>
                        </div>
                    </dl>
                </div>
            </div>
        </div>
    )
};

export default DatabaseDetails;