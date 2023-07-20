import React, { useState, useEffect } from 'react';
import { FaUserCircle } from 'react-icons/fa'; // import account icon from react-icons
import { useDispatch, useSelector } from "react-redux";
import { fetchUser } from "../redux/thunks/userThunks";

function DatabaseOverview() {
    const dispatch = useDispatch();
    const { loading, user } = useSelector(state => state.login);
    const [databases, setDatabases] = useState([
        'Database 1',
        'Database 2',
        'Database 3',
        // ... add your initial databases here
    ]);

    const addDatabase = () => {
        dispatch(fetchUser())
            .then(x => console.log(user))

    };
    const removeDatabase = () => {
        const databaseNameToRemove = prompt('Enter name of database to remove:');
        setDatabases(databases.filter(db => db !== databaseNameToRemove));
    };

    return (
        <div>
            <FaUserCircle size={30} /> {/* account icon */}
            <h1>Database Overview</h1>
            <button onClick={addDatabase}>Add Database</button>
            <button onClick={removeDatabase}>Remove Database</button>
            <ul>
                {databases.map((db, index) => (
                    <li key={index}>{db}</li>
                ))}
            </ul>
        </div>
    );
}

export default DatabaseOverview;
