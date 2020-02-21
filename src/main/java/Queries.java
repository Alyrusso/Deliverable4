package ser322;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Queries { 
	
	/**
	 * queryByCreator prints the track name, track duration, album name, and release date of all audio files by creator queried
	 * @param ctr -creator name
	 * @param conn -connection
	 */
	public void queryByCreator(String ctr, Connection conn) {
		try{
			//create statement
			String stmt = "SELECT ReleaseName, Duration, AlbumName, ReleaseDate"
					+ "FROM album, audiofile, createdby, creator"
					+ "WHERE album.AlbumID = audioFile.AlbumID "
					+ "AND audiofile.TrackID = createdby.TrackID "
					+ "AND createdby.CreatorID = creator.CreatorID"
					+ "AND creator.CreatorID = ?"
					+ "ORDER BY ReleaseDate ASC"; //order results by release date of album
			PreparedStatement pstmt = conn.prepareStatement(stmt);
			pstmt.setString(1, ctr);
			//make query
			ResultSet rs = pstmt.executeQuery();
			//check if results were found
			if(rs.next() == false) {
				System.out.println ("No results found for " + ctr);
			}
			//display results
			else {
				System.out.println("From Creator: " + ctr + "\nAudio File Name:\tDuration:\tAlbum Name:\tReleaseDate:");
				while(rs.next())
					System.out.println(rs.getString(1)+ "\t" +rs.getString(2) + "\t" + rs.getString(3) + "\t" + rs.getString(4));
			}
		}
		catch (Exception exc){
			exc.printStackTrace();
		}
	}
	
	/**
	 * querybyAudioTitle take in track name and prints track name, duration of track, creator name, album name, and release date
	 * @param title - title of audio file queried
	 * @param conn --connection 
	 */
	public void queryByAudioTitle(String title, Connection conn) {
		try{
			//create statement
			String stmt = "SELECT ReleaseName, Duration, creator.Name, AlbumName, ReleaseDate"
					+ "FROM album, audiofile, createdby, creator"
					+ "WHERE album.AlbumID = audioFile.AlbumID "
					+ "AND audiofile.TrackID = createdby.TrackID "
					+ "AND createdby.CreatorID = creator.CreatorID"
					+ "AND audiofile.ReleaseName = ?"
					+ "ORDER BY ReleaseDate ASC"; //if two songs have name order by release date
			PreparedStatement pstmt = conn.prepareStatement(stmt);
			pstmt.setString(1, title);
			//make query
			ResultSet rs = pstmt.executeQuery();
			//check if results were found
			if(rs.next() == false) {
				System.out.println ("No results found for " + title);
			}
			//display results
			else {
				System.out.println("Audio File Name:\tDuration:\tCreator:\tAlbum Name:\tReleaseDate:");
				while(rs.next())
					System.out.println(rs.getString(1)+ "\t" +rs.getString(2) + "\t" + rs.getString(3) + "\t" + rs.getString(4) + "\t" + rs.getString(5));
			}
		}
		catch (Exception exc){
			exc.printStackTrace();
		}
	}
	
	/**
	 * queryByAlbumTitle prints the album name, creator name, total duration of album, release date, label if applicable and 
	 * country if applicable. It then prints track info for tracks in album
	 * @param title - title of album queried
	 * @param conn
	 */
	public void queryByAlbumTitle(String title, Connection conn) {
		try{
			//create statement
			String stmt = "SELECT AlbumName, creator.Name, SUM(audiofile.Duration), ReleaseDate, LabelID, country.Name, audiofile.ReleaseName"
					+ "FROM album, audiofile, createdby, creator, recordlabel, country"
					+ "WHERE album.AlbumID = audioFile.AlbumID "
					+ "AND audiofile.TrackID = createdby.TrackID "
					+ "AND createdby.CreatorID = creator.CreatorID"
					+ "AND recordlabel.LabelID = album.LabelID"
					+ "AND country.CountryID = audiofile.CountryID"
					+ "AND album.AlbumName = ?"
					+ "ORDER BY AlbumName ASC"; //if there are two albums with same name, order results alph
			PreparedStatement pstmt = conn.prepareStatement(stmt);
			pstmt.setString(1, title);
			//make query
			ResultSet rs = pstmt.executeQuery();
			//check if results were found
			if(rs.next() == false) {
				System.out.println ("No results found for " + title);
			}
			//display results
			else {
				System.out.println("Album Name:\tCreator:\tDuration:\tReleaseDate:\tRecord Label:\tCountry");
				//print album info
				while(rs.next()) {
					String currentAlb = rs.getString(1);
					String prevAlb = null;
					//print album info once per album
					if(!prevAlb.equals(currentAlb)) {
						if(rs.getString(5) == null) {
							System.out.println(rs.getString(1)+ "\t" +rs.getString(2) + "\t" + rs.getString(3) + "\t" + 
									rs.getString(4) + "\tno record label\t" + rs.getString(6));
						}
						if(rs.getString(6) == null) {
							System.out.println(rs.getString(1)+ "\t" +rs.getString(2) + "\t" + rs.getString(3) + "\t" + 
									rs.getString(4) + "\t" + rs.getString(5) +"\t no country found");
						}
						else System.out.println(rs.getString(1)+ "\t" +rs.getString(2) + "\t" + rs.getString(3) + "\t" + 
											rs.getString(4) + "\t" + rs.getString(5) +"\t" + rs.getString(6));
					}
					//print track info from album
					queryByAudioTitle(rs.getString(7), conn);
					prevAlb = currentAlb;
				}
			}
		}
		catch (Exception exc){
			exc.printStackTrace();
		}
	}
	
	public void queryByGenre(String gnr, Connection conn) {
		try{
			//create statement
			String stmt = "SELECT ReleaseName, creator.Name,  AlbumName, ReleaseDate"
					+ "FROM album, audiofile, createdby, creator, ingenre, genre"
					+ "WHERE album.AlbumID = audioFile.AlbumID "
					+ "AND audiofile.TrackID = createdby.TrackID "
					+ "AND createdby.CreatorID = creator.CreatorID"
					+ "AND ingenre.TrackID = audiofile.TrackID"
					+ "AND genre.GenreID = ingenre.GenreID"
					+ "AND genre.GenreID = ?"
					+ "ORDER BY ReleaseDate ASC"; //order results by release date of album
			PreparedStatement pstmt = conn.prepareStatement(stmt);
			pstmt.setString(1, gnr);
			//make query
			ResultSet rs = pstmt.executeQuery();
			//check if results were found
			if(rs.next() == false) {
				System.out.println ("No results found for " + gnr);
			}
			//display results
			else {
				System.out.println("From Genre: " + gnr + "\nAudio File Name:\tCreator:\tDuration:\tAlbum Name:\tReleaseDate:");
				while(rs.next())
					System.out.println(rs.getString(1)+ "\t" +rs.getString(2) + "\t" + rs.getString(3) + "\t" + rs.getString(4));
			}
		}
		catch (Exception exc){
			exc.printStackTrace();
		}
	}
}



