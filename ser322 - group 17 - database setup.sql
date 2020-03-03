CREATE SCHEMA adb;


CREATE TABLE adb.country (
        CountryID            int                    NOT NULL        UNIQUE,
        Name                VARCHAR(20)        NOT NULL         UNIQUE,
        PRIMARY KEY (CountryID)
);


CREATE TABLE adb.recordlabel (
        LabelID                int                     NOT NULL        UNIQUE,
        Name                    VARCHAR(20)        NOT NULL    UNIQUE,
        FoundingDate             datetime,
        CountryID                int,
        PRIMARY KEY(LabelID),
        FOREIGN KEY(CountryID) REFERENCES adb.country(CountryID)
);


CREATE TABLE adb.album (
        AlbumID            int                    NOT NULL        UNIQUE,
        AlbumName        VARCHAR(64)        NOT NULL,
        MediaType            VARCHAR(10)        NOT NULL,
        ReleaseDate   datetime,
        LabelID            int,
        PRIMARY KEY (AlbumID),
        FOREIGN KEY (LabelID) REFERENCES adb.recordlabel(LabelID) ON DELETE SET NULL
);


CREATE TABLE adb.audiofile (
        TrackID            int                    NOT NULL        UNIQUE,
        ReleaseName VARCHAR(64)        NOT NULL,
        ExplicitRating  VARCHAR(8)            NOT NULL,
        Duration            int                    NOT NULL,
        CountryID            int,
        AlbumID            int                    NOT NULL,
        PRIMARY KEY (TrackID),
        FOREIGN KEY (CountryID) REFERENCES adb.country(CountryID),
        FOREIGN KEY (AlbumID) REFERENCES adb.album(AlbumID) ON DELETE CASCADE
);


CREATE TABLE adb.creator (
        CreatorID            int                        NOT NULL        UNIQUE,
        Name                VARCHAR(20)            NOT NULL,
        PRIMARY KEY(CreatorID)
);


CREATE TABLE adb.genre (
        GenreID                VARCHAR(20)            NOT NULL        UNIQUE,
        Description            VARCHAR(100),
        PRIMARY KEY(GenreID)
);


CREATE TABLE adb.ingenre (
        TrackID            int                    NOT NULL,
        GenreID            VARCHAR(20)            NOT NULL,
        PRIMARY KEY (TrackID, GenreID),
        FOREIGN KEY (TrackID) REFERENCES adb.audiofile(TrackID) ON DELETE CASCADE,
    FOREIGN KEY (GenreID) REFERENCES adb.genre(GenreID) ON DELETE CASCADE
);


CREATE TABLE adb.createdby (
        TrackID             int                     NOT NULL,
        CreatorID            int                    NOT NULL,
        PRIMARY KEY (TrackID, CreatorID),
        FOREIGN KEY (TrackID) REFERENCES adb.audiofile(TrackID) ON DELETE CASCADE,
        FOREIGN KEY (CreatorID) REFERENCES adb.creator(CreatorID) ON DELETE CASCADE
);