-- Add customer information fields to chats table
ALTER TABLE chats ADD COLUMN customer_name VARCHAR(255);
ALTER TABLE chats ADD COLUMN customer_email VARCHAR(255);
