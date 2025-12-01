-- SQLite v4 to v5 migration
-- SQLite stores all integers as 64-bit, so no type change needed for size column
-- Just add the ext column
ALTER TABLE ftb ADD COLUMN ext TEXT DEFAULT '';
UPDATE vtb SET ver=5;
