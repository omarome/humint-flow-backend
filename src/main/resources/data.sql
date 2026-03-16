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
    ('Helen', 'Taylor', 26, 'helen.taylor@example.com', 'Active', true, NULL, 'employee')
ON CONFLICT (email) DO NOTHING;

