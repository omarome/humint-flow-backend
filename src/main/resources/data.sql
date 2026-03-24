-- Clear old variables
DELETE FROM variables WHERE name IN ('firstName', 'lastName', 'nickname');

-- Seed Variables
INSERT INTO variables (name, label, field_offset, type)
VALUES 
    ('fullName', 'Full Name', 8, 'STRING'),
    ('email', 'Email', 4, 'EMAIL'),
    ('userType', 'User Type', 28, 'STRING'),
    ('age', 'Age', 0, 'UDINT'),
    ('status', 'Account Status', 24, 'STRING'),
    ('isOnline', 'Online Status', 12, 'BOOL')
ON CONFLICT (name) DO UPDATE SET label = EXCLUDED.label;

-- Seed Users
INSERT INTO users (first_name, last_name, age, email, status, is_online, user_type)
VALUES 
    ('John', 'Doe', 28, 'john.doe@example.com', 'Active', true, 'student'),
    ('Jane', 'Smith', 32, 'jane.smith@example.com', 'Active', false, 'employee'),
    ('Bob', 'Johnson', 45, 'bob.johnson@example.com', 'Inactive', false, 'unemployed'),
    ('Alice', 'Williams', 29, 'alice.williams@example.com', 'Active', true, 'student'),
    ('Charlie', 'Brown', 35, 'charlie.brown@example.com', 'Pending', true, 'employee'),
    ('Diana', 'Davis', 27, 'diana.davis@example.com', 'Active', false, 'retired'),
    ('Edward', 'Miller', 41, 'edward.miller@example.com', 'Inactive', false, 'employee'),
    ('Fiona', 'Wilson', 33, 'fiona.wilson@example.com', 'Active', true, 'student'),
    ('George', 'Moore', 38, 'george.moore@example.com', 'Pending', false, 'unemployed'),
    ('Helen', 'Taylor', 26, 'helen.taylor@example.com', 'Active', true, 'employee'),
    ('Ian', 'Wright', 42, 'ian.wright@example.com', 'Active', false, 'retired'),
    ('Julia', 'Roberts', 31, 'julia.roberts@example.com', 'Pending', true, 'employee'),
    ('Kevin', 'Hart', 22, 'kevin.hart@example.com', 'Active', true, 'student'),
    ('Linda', 'Hamilton', 55, 'linda.hamilton@example.com', 'Inactive', false, 'retired'),
    ('Michael', 'Jordan', 39, 'michael.jordan@example.com', 'Active', true, 'employee'),
    ('Nancy', 'Drew', 24, 'nancy.drew@example.com', 'Active', false, 'student'),
    ('Oliver', 'Twist', 48, 'oliver.twist@example.com', 'Pending', false, 'unemployed'),
    ('Pamela', 'Anderson', 36, 'pamela.anderson@example.com', 'Active', true, 'employee'),
    ('Quinn', 'Mallory', 29, 'quinn.mallory@example.com', 'Inactive', false, 'student'),
    ('Rachel', 'Green', 34, 'rachel.green@example.com', 'Active', true, 'employee'),
    ('Steven', 'Spielberg', 62, 'steven.spielberg@example.com', 'Active', false, 'retired'),
    ('Tina', 'Fey', 45, 'tina.fey@example.com', 'Pending', true, 'employee'),
    ('Uma', 'Thurman', 37, 'uma.thurman@example.com', 'Active', false, 'employee'),
    ('Victor', 'Hugo', 51, 'victor.hugo@example.com', 'Inactive', false, 'unemployed'),
    ('Wendy', 'Darling', 21, 'wendy.darling@example.com', 'Active', true, 'student')
ON CONFLICT (email) DO UPDATE 
SET user_type = EXCLUDED.user_type;

