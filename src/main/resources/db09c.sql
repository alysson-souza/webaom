ALTER TABLE jtb ADD COLUMN jobs_visible INTEGER NOT NULL DEFAULT 1;
ALTER TABLE jtb ADD COLUMN alt_visible INTEGER NOT NULL DEFAULT 1;
UPDATE jtb SET jobs_visible=1 WHERE jobs_visible IS NULL;
UPDATE jtb SET alt_visible=1 WHERE alt_visible IS NULL;
UPDATE vtb SET ver=7;
