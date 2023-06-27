import React, { useState } from 'react';

const Databases = () => {
  const [databases, setDatabases] = useState([
    { name: 'Database 1', checked: false },
    { name: 'Database 2', checked: true },
    { name: 'Database 3', checked: false },
  ]);

  const [newDatabaseName, setNewDatabaseName] = useState('');
  const [showAccountDropdown, setAccountDropdown] = useState(false);

  const handleCheckboxChange = (index) => {
    const newDatabases = [...databases];
    newDatabases[index].checked = !newDatabases[index].checked;
    setDatabases(newDatabases);
  };

  const handleDropdownClick = (index) => {
    const newDatabases = [...databases];
    newDatabases[index].dropdownOpen = !newDatabases[index].dropdownOpen;
    setDatabases(newDatabases);
  };

  const handleDeleteClick = () => {
    const newDatabases = databases.filter((database) => !database.checked);
    setDatabases(newDatabases);
  };

  const handleAddDatabase = () => {
    if (newDatabaseName !== '') {
      const newDatabase = { name: newDatabaseName, checked: false };
      setDatabases([...databases, newDatabase]);
      setNewDatabaseName('');
    }
  };

  const handleAccountDropdownClick = () => {

  }

  return (
    <div>
      <div>
        <div className='account-dropdown'
          onClick={() => setAccountDropdown(!showAccountDropdown)}
        >
          <span>Account</span>
        </div>
        {showAccountDropdown && (
          <div className='account-dropdown'>
            <div className="dropdown-menu">
              <div className="dropdown-item">Settings</div>
              <div className="dropdown-item">Logout</div>
            </div>
          </div>
        )}
        <button onClick={handleDeleteClick}>Delete</button>
        <button onClick={handleAddDatabase}>Add Database</button>
        <input
          type="text"
          value={newDatabaseName}
          onChange={(event) => setNewDatabaseName(event.target.value)}
          placeholder="Enter database name"
        />
      </div>
      <table>
        <thead>
          <tr>
            <th></th>
            <th>Name</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {databases.map((database, index) => (
            <tr key={index}>
              <td>
                <input
                  type="checkbox"
                  checked={database.checked}
                  onChange={() => handleCheckboxChange(index)}
                />
              </td>
              <td>{database.name}</td>
              <td>
                <div className="dropdown">
                  <div
                    className="dropdown-toggle"
                    onClick={() => handleDropdownClick(index)}
                  >
                    <span>...</span>
                  </div>
                  {database.dropdownOpen && (
                    <div className="dropdown-menu">
                      <div className="dropdown-item">Option 1</div>
                      <div className="dropdown-item">Option 2</div>
                      <div className="dropdown-item">Option 3</div>
                    </div>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default Databases;
