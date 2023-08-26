import React, { useState } from 'react';
import securedApi from '../api/securedApi';
import unsecuredApi from '../api/unsecuredApi';

const NewDbModal = ({ handleCloseModal, addDatabase }) => {

    const [password, setPassword] = useState("");

    const [newDbName, setNewDbName] = useState("");
    const [error, setError] = useState("");
    const handleAddDatabase = () => {
        addDatabase(newDbName, password)
        handleCloseModal()
    };

    const doesDatabaseExist = (name) => {

        securedApi.get('/redis/exists/' + name)
            .then(response => {
                if (response.data) {
                    setError('A database with this name already exists.');
                } else {
                    setError('');
                }
            })
    };




    return (
        <div className="fixed z-10 inset-0 overflow-y-auto">
            <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
                <div className="fixed inset-0 transition-opacity" aria-hidden="true" onClick={handleCloseModal}>
                    <div className="absolute inset-0 bg-gray-500 opacity-75"></div>
                </div>

                <div className="inline-block align-bottom bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full sm:p-6">
                    <div>
                        <div className="mt-3 text-center sm:mt-5">
                            <h3 className="text-lg leading-6 font-medium text-gray-900" id="modal-headline">
                                Add a new database
                            </h3>
                            <div className="mt-2">
                                <input
                                    type="text"
                                    value={newDbName}
                                    onChange={(e) => {
                                        setNewDbName(e.target.value);
                                        if (e.target.value.length > 0) {
                                            doesDatabaseExist(e.target.value)
                                        } else {
                                            setError('');
                                        }
                                    }}
                                    placeholder="Enter new database name"
                                    className="w-full px-3 py-2 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:shadow-outline-blue focus:border-blue-300 transition duration-150 ease-in-out"
                                />
                                {error && <p className="text-red-500">{error}</p>}
                            </div>
                            
                            <div className="mt-2">
                                <input
                                    type="password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    placeholder="Password (Optional)"
                                    className="w-full px-3 py-2 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:shadow-outline-blue focus:border-blue-300 transition duration-150 ease-in-out"
                                />
                            </div>
                        </div>
                    </div>


                    <div className="mt-5 sm:mt-6">
                        <span className="flex w-full rounded-md shadow-sm">
                            <button
                                type="button"
                                className="inline-flex justify-center w-full px-4 py-2 text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:border-blue-700 focus:shadow-outline-blue transition ease-in-out duration-150 sm:text-sm sm:leading-5"
                                onClick={handleAddDatabase}
                                disabled={newDbName.length === 0 || error.length > 0}
                            >
                                Submit
                            </button>
                        </span>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default NewDbModal