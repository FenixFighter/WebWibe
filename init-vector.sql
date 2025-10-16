-- Enable the vector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create the knowledge_vectors table with vector support
CREATE TABLE IF NOT EXISTS knowledge_vectors (
    id BIGSERIAL PRIMARY KEY,
    question TEXT,
    answer TEXT,
    category VARCHAR(255),
    embedding TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for vector similarity search
CREATE INDEX IF NOT EXISTS knowledge_vectors_embedding_idx 
ON knowledge_vectors USING ivfflat (embedding vector_cosine_ops) 
WITH (lists = 100);

-- Create index for category filtering
CREATE INDEX IF NOT EXISTS knowledge_vectors_category_idx 
ON knowledge_vectors (category);
