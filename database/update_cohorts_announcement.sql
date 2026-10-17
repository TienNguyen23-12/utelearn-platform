ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS announcement_title VARCHAR(255);
ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS announcement_content TEXT;
