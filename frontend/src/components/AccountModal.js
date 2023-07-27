import React from 'react';
import { useSelector } from "react-redux";


const AccountModal = ({ handleCloseModal }) => {
    const user = useSelector(state => state.login.user);

    return (
        <div className="fixed z-10 inset-0 overflow-y-auto">
            <div className="h-screen bg-gray-100 flex items-center justify-center">
                <div className="bg-white shadow overflow-hidden sm:rounded-lg max-w-xl mx-auto mt-10">
                    <div className="flex justify-between items-center px-4 py-5 sm:px-6">
                        <h3 className="text-lg leading-6 font-medium text-gray-900">
                            Account Info
                        </h3>
                        <button
                            onClick={handleCloseModal}
                            className="bg-gray-300 hover:bg-gray-400 text-gray-800 font-bold py-1 px-2 rounded inline-flex items-center"
                        >
                            Close
                        </button>
                    </div>
                    <div className="border-t border-gray-200">
                        <dl>
                            {infoRow("Username", user.username)}
                            {infoRow("Firsname", user.firstName)}
                            {infoRow("Lasrtname", user.lastName)}
                            {infoRow("Email", user.mail)}
                        </dl>
                    </div>
                </div>
            </div>
        </div>
    );
};


function infoRow(text, value) {
    return (
        <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
            <dt className="text-sm font-medium text-gray-500">
            {text}
            </dt>
            <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                {value}
            </dd>
        </div>
    );
}

export default AccountModal; 