ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS visibility VARCHAR(30) DEFAULT 'PUBLIC';

CREATE INDEX IF NOT EXISTS idx_cohorts_creator ON cohorts(created_by);
CREATE INDEX IF NOT EXISTS idx_cohorts_visibility ON cohorts(visibility);

UPDATE cohorts SET created_by = 2;

UPDATE cohorts 
SET visibility = 'PUBLIC' 
WHERE visibility IS NULL;
