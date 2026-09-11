-- Update admin password to admin123 (for existing databases)
UPDATE sys_user SET password = '$2b$10$PH6B00sby891aNNi/c4n5uEc3MkPHtsMauz8M68Xj6VvtUkVAZ2YO' WHERE username = 'admin';
