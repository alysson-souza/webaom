-- SQLite Database Schema for WebAOM
-- This is a SQLite-compatible version of the PostgreSQL/MySQL schema
-- Main change: Uses INTEGER PRIMARY KEY for auto-increment (implicit ROWID)

CREATE TABLE vtb
(
    ver INTEGER NOT NULL
);

CREATE TABLE atb
(
    aid      INTEGER      NOT NULL,
    time     TEXT         NOT NULL DEFAULT (datetime('now')),
    romaji   TEXT         NOT NULL,
    kanji    TEXT                  DEFAULT NULL,
    english  TEXT                  DEFAULT NULL,
    year     INTEGER      NOT NULL,
    episodes INTEGER      NOT NULL,
    last_ep  INTEGER      NOT NULL,
    type     TEXT         NOT NULL,
    genre    TEXT         NOT NULL,
    img      INTEGER               DEFAULT 0,
    PRIMARY KEY (aid)
);

CREATE TABLE etb
(
    eid     INTEGER      NOT NULL,
    english TEXT         NOT NULL,
    kanji   TEXT                  DEFAULT NULL,
    romaji  TEXT                  DEFAULT NULL,
    number  TEXT         NOT NULL,
    time    TEXT         NOT NULL DEFAULT (datetime('now')),
    PRIMARY KEY (eid)
);

CREATE TABLE gtb
(
    gid   INTEGER      NOT NULL,
    time  TEXT         NOT NULL DEFAULT (datetime('now')),
    name  TEXT         NOT NULL,
    short TEXT         NOT NULL,
    PRIMARY KEY (gid)
);

CREATE TABLE ftb
(
    fid        INTEGER      NOT NULL,
    aid        INTEGER      NOT NULL,
    eid        INTEGER      NOT NULL,
    gid        INTEGER      NOT NULL,
    state      INTEGER      NOT NULL,
    size       INTEGER      NOT NULL,
    len        INTEGER      NOT NULL DEFAULT 0,
    ed2k       TEXT                  DEFAULT NULL,
    md5        TEXT                  DEFAULT NULL,
    sha1       TEXT                  DEFAULT NULL,
    crc32      TEXT                  DEFAULT NULL,
    ripsource  TEXT                  DEFAULT NULL,
    quality    TEXT                  DEFAULT NULL,
    audio      TEXT                  DEFAULT NULL,
    video      TEXT                  DEFAULT NULL,
    resolution TEXT                  DEFAULT NULL,
    def_name   TEXT         NOT NULL,
    time       TEXT         NOT NULL DEFAULT (datetime('now')),
    sublang    TEXT         NOT NULL,
    dublang    TEXT         NOT NULL,
    ext        TEXT                  DEFAULT '',
    PRIMARY KEY (fid)
);

CREATE TABLE utb
(
    uid  INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    UNIQUE (name)
);

CREATE TABLE dtb
(
    did  INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    UNIQUE (name)
);

CREATE TABLE jtb
(
    orig   TEXT    NOT NULL,
    name   TEXT    NOT NULL,
    did    INTEGER NOT NULL,
    fid    INTEGER NOT NULL,
    status INTEGER NOT NULL,
    ed2k   TEXT    NOT NULL,
    md5    TEXT             DEFAULT NULL,
    sha1   TEXT             DEFAULT NULL,
    tth    TEXT             DEFAULT NULL,
    crc32  TEXT             DEFAULT NULL,
    size   INTEGER NOT NULL,
    uid    INTEGER NOT NULL,
    lid    INTEGER NOT NULL,
    time   TEXT    NOT NULL DEFAULT (datetime('now')),
    avxml  TEXT             DEFAULT NULL,
    PRIMARY KEY (size, ed2k),
    FOREIGN KEY (did) REFERENCES dtb (did),
    FOREIGN KEY (fid) REFERENCES ftb (fid),
    FOREIGN KEY (uid) REFERENCES utb (uid)
);

CREATE INDEX i_dtb_name ON dtb (name);
CREATE INDEX i_jtb_name ON jtb (name);

INSERT INTO ftb (fid, aid, eid, gid, state, size, ed2k, def_name, sublang, dublang)
VALUES (0, 0, 0, 0, 0, 0, 'ed2k', '', '', '');
INSERT INTO etb (eid, english, number)
VALUES (0, '', '');
INSERT INTO utb (name)
VALUES ('default');
INSERT INTO vtb
VALUES (6);
