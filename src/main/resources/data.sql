-- Seed Variables
INSERT INTO variables (name, label, field_offset, type)
VALUES 
    ('age', 'Age', 0, 'UDINT'),
    ('email', 'Email', 4, 'EMAIL'),
    ('firstName', 'First Name', 8, 'STRING'),
    ('isOnline', 'Is Online', 12, 'BOOL'),
    ('lastName', 'Last Name', 16, 'STRING'),
    ('nickname', 'Nickname', 20, 'STRING'),
    ('status', 'Status', 24, 'STRING'),
    ('userType', 'User Type', 28, 'STRING')
ON CONFLICT (name) DO NOTHING;

-- Seed Users
INSERT INTO users (first_name, last_name, age, email, status, is_online, nickname, user_type)
VALUES 
    ('John', 'Doe', 28, 'john.doe@example.com', 'Active', true, 'Johnny', 'student'),
    ('Jane', 'Smith', 32, 'jane.smith@example.com', 'Active', false, NULL, 'employee'),
    ('Bob', 'Johnson', 45, 'bob.johnson@example.com', 'Inactive', false, 'Bobby', 'unemployed'),
    ('Alice', 'Williams', 29, 'alice.williams@example.com', 'Active', true, NULL, 'student'),
    ('Charlie', 'Brown', 35, 'charlie.brown@example.com', 'Pending', true, 'Chuck', 'employee'),
    ('Diana', 'Davis', 27, 'diana.davis@example.com', 'Active', false, NULL, 'retired'),
    ('Edward', 'Miller', 41, 'edward.miller@example.com', 'Inactive', false, 'Ed', 'employee'),
    ('Fiona', 'Wilson', 33, 'fiona.wilson@example.com', 'Active', true, NULL, 'student'),
    ('George', 'Moore', 38, 'george.moore@example.com', 'Pending', false, 'Geo', 'unemployed'),
    ('Helen', 'Taylor', 26, 'helen.taylor@example.com', 'Active', true, NULL, 'employee'),
    ('Ian', 'Wright', 42, 'ian.wright@example.com', 'Active', false, 'Wrighty', 'retired'),
    ('Julia', 'Roberts', 31, 'julia.roberts@example.com', 'Pending', true, 'Jules', 'employee'),
    ('Kevin', 'Hart', 22, 'kevin.hart@example.com', 'Active', true, 'Kev', 'student'),
    ('Linda', 'Hamilton', 55, 'linda.hamilton@example.com', 'Inactive', false, NULL, 'retired'),
    ('Michael', 'Jordan', 39, 'michael.jordan@example.com', 'Active', true, 'MJ', 'employee'),
    ('Nancy', 'Drew', 24, 'nancy.drew@example.com', 'Active', false, 'Nan', 'student'),
    ('Oliver', 'Twist', 48, 'oliver.twist@example.com', 'Pending', false, 'Ollie', 'unemployed'),
    ('Pamela', 'Anderson', 36, 'pamela.anderson@example.com', 'Active', true, 'Pam', 'employee'),
    ('Quinn', 'Mallory', 29, 'quinn.mallory@example.com', 'Inactive', false, 'Q', 'student'),
    ('Rachel', 'Green', 34, 'rachel.green@example.com', 'Active', true, 'Rach', 'employee'),
    ('Steven', 'Spielberg', 62, 'steven.spielberg@example.com', 'Active', false, 'Steve', 'retired'),
    ('Tina', 'Fey', 45, 'tina.fey@example.com', 'Pending', true, NULL, 'employee'),
    ('Uma', 'Thurman', 37, 'uma.thurman@example.com', 'Active', false, NULL, 'employee'),
    ('Victor', 'Hugo', 51, 'victor.hugo@example.com', 'Inactive', false, 'Vic', 'unemployed'),
    ('Wendy', 'Darling', 21, 'wendy.darling@example.com', 'Active', true, 'Wen', 'student')
ON CONFLICT (email) DO UPDATE 
SET user_type = EXCLUDED.user_type;

