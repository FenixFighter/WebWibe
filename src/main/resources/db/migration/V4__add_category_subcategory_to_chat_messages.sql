-- Add category and subcategory fields to chat_messages table
ALTER TABLE chat_messages 
ADD COLUMN category VARCHAR(255),
ADD COLUMN subcategory VARCHAR(255);
