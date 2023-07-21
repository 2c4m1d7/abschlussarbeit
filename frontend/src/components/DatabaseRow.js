import React from 'react';

const DatabaseRow = ({ db, index, handleDatabaseSelection, isSelected, onClick }) => (
    <div key={db.id}
        className="flex items-center space-x-3 bg-gray-200 w-full p-4 rounded-lg"
        onClick={onClick}> {/* Added classes here */}
        <input
            type="checkbox"
            onChange={() => handleDatabaseSelection(db.id)}
            checked={isSelected}
            className="form-checkbox h-5 w-5 text-blue-600 rounded-lg focus:ring-blue-200 focus:outline-none"
        />
        <label className="text-grey-700 text-sm">{db.name}</label>
    </div>
);

export default DatabaseRow;
