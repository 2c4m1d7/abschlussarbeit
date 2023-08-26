import React, { useState, useEffect } from 'react';
import { FaDatabase } from 'react-icons/fa'; // import account icon from react-icons
import { useDispatch, useSelector } from "react-redux";
import { fetchUser } from "../redux/thunks/userThunks";
import { AiOutlineUser } from 'react-icons/ai';
import { MdDelete, MdLogout } from 'react-icons/md';
import { IoMdAddCircleOutline } from 'react-icons/io';
import DatabaseRow from '../components/DatabaseRow';
import { logout } from '../redux/slices/userSlice';
import { useNavigate } from 'react-router-dom';
import securedApi from '../api/securedApi';
import Header from '../components/Header';
import NewDbModal from '../components/NewDbModal';
import AccountModal from '../components/AccountModal';



function MainPage() {
    const navigate = useNavigate();
    const { loading, user } = useSelector(state => state.user);
    const [databases, setDatabases] = useState([]);
    const [selectedDatabases, setSelectedDatabases] = useState([]);

    const [isAccountModalOpen, setIsAccountModalOpen] = useState(false); 


    const handleOpenAccountModal = () => setIsAccountModalOpen(true); 
    const handleCloseAccountModal = () => setIsAccountModalOpen(false); 

    const [isModalOpen, setIsModalOpen] = useState(false);
    const handleOpenModal = () => {
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
    };



    const fetchDbs = () => {
        securedApi.get('databases')
            .then(response => {
                setDatabases(response.data.databases)
            })
            .catch(error => {
                console.log(error)
            })
    }
    useEffect(() => {
        fetchDbs()
    }, [])

    const addDatabase = (newDbName, password) => {
        securedApi.post('/redis/database', { dbName: newDbName, password: password })
            .then(response => {
                fetchDbs()
            })
            .catch(error => {
                console.log(error)
            })
    };

    const handleDatabaseSelection = (index) => {
        if (selectedDatabases.includes(index)) {
            setSelectedDatabases(selectedDatabases.filter((i) => i !== index));
        } else {
            setSelectedDatabases([...selectedDatabases, index]);
        }
    };
    const handleRowClick = (db) => {
        navigate(`/database/${db.id}`);
    }
    const handleRemoveDatabases = () => {

        securedApi.delete('redis/delete-batch', { data: selectedDatabases })
            .then(response => {
                setSelectedDatabases([]);
                fetchDbs()
            })
            .catch(error => {
                console.log(error)
            })
    }

    const handleSelectAllDatabases = () => {
        const allDatabaseIds = databases.map(db => db.id);

        if (selectedDatabases.length === allDatabaseIds.length) {
            setSelectedDatabases([]);
        } else {
            setSelectedDatabases(allDatabaseIds);
        }
    };
    return (

        <div className="h-screen bg-gray-100 flex items-center justify-center flex-col">
            <Header handleOpenAccountModal={handleOpenAccountModal} />

            {isModalOpen && (
                <NewDbModal handleCloseModal={handleCloseModal} addDatabase={addDatabase} />
            )}

            {isAccountModalOpen && (
                <AccountModal handleCloseModal={handleCloseAccountModal} />
            )}


            <main className="overflow-y-auto mx-auto max-w-lg bg-white p-8 mt-7 rounded-xl shadow-md ">
                <section className="flex items-center space-x-4 mb-7">
                    <h1 className="text-gray-800 text-xl font-semibold">Redis Databases</h1>
                    <FaDatabase size={38} className="text-blue-600" />
                </section>

                <section className="my-4">
                    <div className="flex space-x-4 mb-4">
                        <button onClick={handleOpenModal} className="px-4 py-2 bg-blue-600 text-white rounded-lg">
                            <IoMdAddCircleOutline className="inline-block mr-2" /> Add Database
                        </button>

                        <button onClick={handleRemoveDatabases} disabled={selectedDatabases.length === 0} className="px-4 py-2 bg-red-600 text-white rounded-lg">
                            <MdDelete className="inline-block mr-2" /> Remove
                        </button>
                    </div>

                    {databases.length > 0 && (
                        <button onClick={handleSelectAllDatabases} className="px-4 py-2 bg-green-600 text-white rounded-lg w-full mb-4">
                            {selectedDatabases.length === databases.length ? "Deselect All" : "Select All"}
                        </button>
                    )}
                    <div className="space-y-2 overflow-y-auto h-60">
                        {databases.map((db) => (
                            <DatabaseRow
                                db={db}
                                handleDatabaseSelection={handleDatabaseSelection}
                                isSelected={selectedDatabases.includes(db.id)}
                                onClick={() => handleRowClick(db)}
                            />
                        ))}
                    </div>
                </section>
            </main>
        </div>
    );

}


export default MainPage;
