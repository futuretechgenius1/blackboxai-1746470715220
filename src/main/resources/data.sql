-- Insert default roles
INSERT INTO roles (name) VALUES ('ADMIN') ON DUPLICATE KEY UPDATE name = name;
INSERT INTO roles (name) VALUES ('USER') ON DUPLICATE KEY UPDATE name = name;

-- Insert admin user (password: Admin@123)
INSERT INTO users (name, email, password, enabled) 
VALUES ('Admin', 'admin@gstbilling.com', '$2a$10$3WrePoMsQHzYYCUZNYKJluJXZF/ZKpJ8kK8XiE8DIqyHUYoQAnj8.', true)
ON DUPLICATE KEY UPDATE email = email;

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'admin@gstbilling.com' AND r.name = 'ADMIN'
ON DUPLICATE KEY UPDATE user_id = user_id;
