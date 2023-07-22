import React from 'react';

const DatabaseRow = ({ db, handleDatabaseSelection, isSelected, onClick }) => (
    <div key={db.id}
        className="flex items-center space-x-3 bg-gray-200 w-full p-4 rounded-lg"
        onClick={onClick}
        > 
        <input
            type="checkbox"
            onClick={(e) => {
                e.stopPropagation();  
                handleDatabaseSelection(db.id);
            }}
            onChange={(e) => {
                // e.stopPropagation();        // Prevent row click handler when checkbox is clicked
                // handleDatabaseSelection(db.id);
            }} 
            checked={isSelected}
            className="form-checkbox h-5 w-5 text-blue-600 rounded-lg focus:ring-blue-200 focus:outline-none"
        />
        <label className="text-grey-700 text-sm">{db.name}</label>
    </div>
);

export default DatabaseRow;
