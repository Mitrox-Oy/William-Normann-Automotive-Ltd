-- Persist uploaded images in Postgres so they survive dyno/container restarts.

CREATE TABLE IF NOT EXISTS uploaded_images (
    file_name VARCHAR(255) PRIMARY KEY,
    image_data BYTEA NOT NULL,
    content_type VARCHAR(100),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_uploaded_images_created_date ON uploaded_images(created_date);
