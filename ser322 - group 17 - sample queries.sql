USE adb;

/* retrieve all audio files and their related data */
SELECT ReleaseName, sec_to_time(audiofile.Duration) AS Duration, AlbumName AS Album, ReleaseDate, creator.Name AS Artist, GenreId AS Genre, country.Name AS Country, recordlabel.Name AS Label
FROM adb.audiofile, adb.album, adb.createdby, adb.creator, adb.ingenre, adb.country, adb.recordlabel
WHERE album.AlbumID = audiofile.AlbumID
AND createdby.CreatorID = creator.CreatorID
AND createdby.TrackID = audiofile.TrackID
AND ingenre.TrackID = audiofile.TrackID
AND audiofile.CountryID = country.CountryID
AND album.LabelID = recordlabel.LabelID;


/* find all song titles from creator 'Ratatat' */
SELECT ReleaseName
FROM audiofile, createdby, creator
WHERE audiofile.TrackID = createdby.TrackID 
AND createdby.CreatorID=creator.CreatorID         
AND creator.Name = 'Ratatat';


/* find description of genre(s) of album 'In the Court of the Crimson King' */
SELECT DISTINCT genre.GenreID, Description
FROM audiofile, ingenre, genre, album
WHERE album.AlbumID = audiofile.AlbumID
AND ingenre.TrackID = audiofile.TrackID
AND ingenre.GenreID = genre.GenreID
AND album.AlbumName = 'In the Court of the Crimson King';


/* find all song titles and their creators in genre 'Rock' ordered by release date of audio file*/
SELECT ReleaseName, creator.Name AS Creator
FROM audiofile, createdby, creator, genre, ingenre, album
WHERE audiofile.TrackID = createdby.TrackID 
AND createdby.CreatorID=creator.CreatorID 
AND audiofile.TrackID = ingenre.TrackID
AND genre.GenreID = ingenre.GenreID
AND audiofile.albumId = album.albumID
AND genre.genreID = 'Rock'
ORDER BY album.ReleaseDate ASC;


/* find all album names and their release dates from creator  'Kimbra' */
SELECT DISTINCT AlbumName, ReleaseDate, sec_to_time(sum(duration)) AS TotalLength
FROM album, audiofile, createdby, creator
WHERE audiofile.TrackID = createdby.TrackID 
AND createdby.CreatorID = creator.CreatorID 
AND audiofile.albumId = album.albumID
AND creator.Name = 'Kimbra';


/* find all albums and their creators from record label 'Ninja Tune' */
SELECT DISTINCT AlbumName, creator.Name AS Creator, sec_to_time(sum(duration)) AS TotalDuration
FROM recordlabel, album, audiofile, createdby, creator
WHERE recordlabel.LabelID = album. LabelID
AND album.AlbumID = audiofile.AlbumID
AND audiofile.TrackID = createdby.TrackID
AND createdby.CreatorID = creator.CreatorID
AND recordlabel.Name = 'Ninja Tune';


/* find the album title and creator name of song  'Drive It Like You Stole It' */
SELECT AlbumName, creator.Name AS Creator
FROM album, audiofile, createdby, creator
WHERE album.AlbumID = audiofile.AlbumID
AND audiofile.TrackID = createdby.TrackID
AND createdby.CreatorID = creator.CreatorID
AND audiofile.ReleaseName = 'Drive It Like You Stole It';


/*find all song titles in album 'Shriek' */
SELECT ReleaseName
FROM audiofile, album
WHERE audiofile.AlbumID=album.AlbumID
AND album.AlbumName='Shriek';


/*find all song titles from country 'Japan' */
SELECT ReleaseName
FROM audiofile, country
WHERE audiofile.CountryID=country.CountryID
 AND country.name='Japan';


/*find all recordLabels and their ids in country 'USA' */
SELECT recordlabel.Name, recordlabel.LabelID
FROM recordlabel, country
WHERE recordlabel.CountryID=country.CountryID
         AND country.name='USA';




/*Find all track info for songs in album 'Shriek'*/
SELECT audiofile.TrackID, audiofile.ReleaseName, audiofile.ExplicitRating, sec_to_time(audiofile.Duration) AS Duration, audiofile.CountryID, audiofile.AlbumID
FROM audiofile, album
WHERE album.AlbumName='Shriek'
          AND audiofile.AlbumID=album.AlbumID;


/*Get number of tracks in album ‘Sucker Punch’*/
SELECT COUNT(audiofile.TrackID)
FROM audiofile, album
Where audiofile.AlbumID=album.AlbumID
          AND album.AlbumName='Sucker Punch';


/*Get average track duration for album ‘Sucker Punch’*/
SELECT AVG(audiofile.Duration)
FROM audiofile, album
Where audiofile.AlbumID=album.AlbumID
          AND album.AlbumName='Sucker Punch';