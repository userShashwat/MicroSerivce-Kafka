-- Drop databases if they exist
DROP DATABASE IF EXISTS user_db;
DROP DATABASE IF EXISTS order_db;
DROP DATABASE IF EXISTS notification_db;

-- Recreate empty databases
CREATE DATABASE user_db;
CREATE DATABASE order_db;
CREATE DATABASE notification_db;

-- Verify they're empty
SHOW DATABASES;