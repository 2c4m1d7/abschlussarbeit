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
import secureApi from '../api/secureApi';
import Header from '../components/Header';
function MainPage() {
    const navigate = useNavigate();
    const dispatch = useDispatch();
    const { loading, user } = useSelector(state => state.login);
    const [databases, setDatabases] = useState([]);
    const [selectedDatabases, setSelectedDatabases] = useState([]);
    const fetchDbs = () => {
        secureApi.get('databases')
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


    const addDatabase = () => {
        secureApi.post('/redis/database?dbName=testDb')
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

        secureApi.delete('redis/delete-batch', { data: selectedDatabases })
            .then(response => {
                setSelectedDatabases([]);
                fetchDbs()
            })
            .catch(error => {
                console.log(error)
            })
    }
    return (
        <div className="h-screen bg-gray-100 flex items-center justify-center">
            <Header />

            <main className="mx-auto max-w-lg bg-white p-8 mt-7 rounded-xl shadow-md">
                <section className="flex items-center space-x-4 mb-7">
                    <h1 className="text-gray-800 text-xl font-semibold">Redis Databases</h1>
                    <FaDatabase size={38} className="text-blue-600" />
                </section>

                <section className="my-4">
                    <button onClick={addDatabase} className="px-4 py-2 bg-blue-600 text-white rounded-lg mb-4">
                        <IoMdAddCircleOutline className="inline-block mr-2" /> Add Database
                    </button>

                    <button onClick={handleRemoveDatabases} disabled={selectedDatabases.length === 0} className="px-4 py-2 bg-red-600 text-white rounded-lg mb-4 ml-4"> {/* This is the new remove databases button */}
                        <MdDelete className="inline-block mr-2" /> Remove
                    </button>

                    <div className="space-y-2">
                        {databases.map((db, index) => (
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
