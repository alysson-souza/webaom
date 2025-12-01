-- SQLite v5 to v6 migration
-- SQLite stores all integers as 64-bit, so no type change needed for size column
-- No schema changes required, just update version
UPDATE vtb SET ver=6;
