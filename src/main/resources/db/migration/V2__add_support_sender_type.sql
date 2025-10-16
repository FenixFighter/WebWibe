-- Add SUPPORT to sender_type constraint
ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chat_messages_sender_type_check;
ALTER TABLE chat_messages ADD CONSTRAINT chat_messages_sender_type_check 
    CHECK (sender_type IN ('USER', 'AI', 'SUPPORT'));
