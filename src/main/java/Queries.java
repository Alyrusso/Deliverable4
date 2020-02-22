package main.java;

import java.sql.*;
import java.util.Random;

public class Queries {
	private Connection conn;
	private Statement statement;
	private PreparedStatement pStatement;
	private Random rand;

	public Queries(Connection c) {
		this.conn = c;
		this.rand = new Random();
	}
	
	/**
	 * queryByCreator prints the track name, track duration, album name, and release date of all audio files by creator queried
	 * @param ctr -creator name
	 */
	public void queryByCreator(String ctr) {
		try{
			//create statement
			String stmt = "SELECT ReleaseName, Duration, AlbumName, ReleaseDate "
					+ "FROM album, audiofile, createdby, creator "
					+ "WHERE album.AlbumID = audioFile.AlbumID "
					+ "AND audiofile.TrackID = createdby.TrackID "
					+ "AND createdby.CreatorID = creator.CreatorID "
					+ "AND creator.CreatorID = ? "
					+ "ORDER BY ReleaseDate ASC;"; //order results by release date of album
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
	 */
	public void queryByAudioTitle(String title) {
		try{
			//create statement
			String stmt = "SELECT ReleaseName, Duration, creator.Name, AlbumName, ReleaseDate "
					+ "FROM album, audiofile, createdby, creator "
					+ "WHERE album.AlbumID = audioFile.AlbumID "
					+ "AND audiofile.TrackID = createdby.TrackID "
					+ "AND createdby.CreatorID = creator.CreatorID "
					+ "AND audiofile.ReleaseName = ? "
					+ "ORDER BY ReleaseDate ASC;"; //if two songs have name order by release date
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
				do {
					System.out.println(rs.getString(1)+ "\t" +rs.getString(2) + "\t" + rs.getString(3) + "\t" + rs.getString(4) + "\t" + rs.getString(5));
				} while(rs.next());
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
	 */
	public void queryByAlbumTitle(String title) {
		try{
			//create statement
			String stmt = "SELECT AlbumName, creator.Name, SUM(audiofile.Duration), ReleaseDate, recordlabel.LabelID, country.Name, audiofile.ReleaseName "
					+ "FROM album, audiofile, createdby, creator, recordlabel, country "
					+ "WHERE album.AlbumID = audioFile.AlbumID "
					+ "AND audiofile.TrackID = createdby.TrackID "
					+ "AND createdby.CreatorID = creator.CreatorID "
					+ "AND recordlabel.LabelID = album.LabelID "
					+ "AND country.CountryID = audiofile.CountryID "
					+ "AND album.AlbumName = ? "
					+ "ORDER BY AlbumName ASC;"; //if there are two albums with same name, order results alph
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
				do {
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
					queryByAudioTitle(rs.getString(7));
					prevAlb = currentAlb;
				} while(rs.next());
			}
		}
		catch (Exception exc){
			exc.printStackTrace();
		}
	}
	
	public void queryByGenre(String gnr) {
		try{
			//create statement
			String stmt = "SELECT ReleaseName, creator.Name,  AlbumName, ReleaseDate "
					+ "FROM album, audiofile, createdby, creator, ingenre, genre "
					+ "WHERE album.AlbumID = audioFile.AlbumID "
					+ "AND audiofile.TrackID = createdby.TrackID "
					+ "AND createdby.CreatorID = creator.CreatorID "
					+ "AND ingenre.TrackID = audiofile.TrackID "
					+ "AND genre.GenreID = ingenre.GenreID "
					+ "AND genre.GenreID = ? "
					+ "ORDER BY ReleaseDate ASC;"; //order results by release date of album
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
				do {
					System.out.println(rs.getString(1)+ "\t" +rs.getString(2) + "\t" + rs.getString(3) + "\t" + rs.getString(4));
				} while(rs.next());
			}
		}
		catch (Exception exc){
			exc.printStackTrace();
		}
	}


	/**
	 * Get the average track duration for all tracks in a user specified album
	 *
	 * @param alb
	 */
	public void getAvgTrackDurationAlbum(String alb){
		ResultSet rs = null;
		PreparedStatement p_stmt = null;

		try{
			//setup rs and p_stmt
			p_stmt = conn.prepareStatement(
					"SELECT AVG(audiofile.Duration)" +
					" FROM audiofile, album WHERE audiofile.AlbumID=album.AlbumID" +
					" AND album.AlbumName='?';");

			p_stmt.setString(1, alb);

			rs = p_stmt.executeQuery();

			if(rs.next()){
				System.out.println("The average track duration for album '" + alb +"' is: " + rs.getInt(1));
			}else{
				System.out.println("~~ERROR: Album '" + alb + "' returned no result ~~");
			}

		}catch(Exception exc){
			exc.printStackTrace();
		}finally{
			try{
				if(rs != null)
					rs.close();
				if(p_stmt != null)
					p_stmt.close();
			}catch(SQLException se){
				se.printStackTrace();
			}
		}
	}


	/**
	 * Get total number of tracks in a user specified album
	 *
	 * @param alb
	 * @param conn
	 */
	public void numTracksInAlbum(String alb, Connection conn){
		ResultSet rs = null;
		PreparedStatement p_stmt = null;

		try{
			//setup rs and p_stmt
			p_stmt = conn.prepareStatement(
					"SELECT COUNT(audiofile.TrackID)" +
					" FROM audiofile, album WHERE audiofile.AlbumID=album.AlbumID" +
					" AND album.AlbumName='?';");

			p_stmt.setString(1, alb);

			rs = p_stmt.executeQuery();

			if(rs.next()) {
				System.out.println("Total track count for '" + alb + "' is: " + rs.getInt(1));
			}
			else{
				System.out.println("~~ERROR: Album '" + alb + "' returned no result ~~");
			}

		}catch(Exception exc){
			exc.printStackTrace();
		}finally{
			try{
				if(rs != null)
					rs.close();
				if(p_stmt != null)
					p_stmt.close();
			}catch(SQLException se){
				se.printStackTrace();
			}
		}
	}

	/** Inserts an album into the database.
	 * @param albumName Name of the album to insert
	 * @param date  Date the album came out. Format as a number like "20171231". Can be null.
	 * @param label Name of the record label. Inserts label name to DB if not already present. Can be null.
	 * @return the albumID of the inserted album. -1 if insert failed.
	 */
	public int insertAlbum(String albumName, String date, String label) {
		int albumID = -1;
		int labelID = 0;
		if (label != null) labelID = insertRecordLabel(label, null, 0); //
		try {
			pStatement = conn.prepareStatement(
					"INSERT INTO adb.album (AlbumID, AlbumName, MediaType, ReleaseDate, LabelID) " +
					" VALUES (?, ?, 'Music', ?, ?);");
			albumID = getID(albumName);
			pStatement.setInt(1, albumID);
			pStatement.setString(2, albumName);
			if (date != null) pStatement.setString(3, date);
			else pStatement.setNull(3, Types.DATE);
			if (labelID > 0) pStatement.setInt(4, labelID);
			else pStatement.setNull(4, Types.INTEGER);

			pStatement.execute();
			conn.commit();

		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
		return albumID;
	}

	/** Inserts label into DB if not already present, then returns the labelID
	 *
	 * @param label Name of the label.
	 * @param date Founding date of the label. Can be null.
	 * @param countryID Country code the label was founded in. Can be null.
	 * @return The labelID for the new label (or existing labelID if already present). -1 indicates error
	 */
	public int insertRecordLabel(String label, String date, int countryID) {
		int labelID = getRecordLabelID(label);
		if (labelID == 0) {
			try {
				pStatement = conn.prepareStatement(
						"INSERT INTO adb.recordlabel (LabelID, Name, FoundingDate, CountryID)" +
						" VALUES (?, ?, ?, ?);");
				labelID = getID(label);
				pStatement.setInt(1, labelID);
				pStatement.setString(2, label);
				if (date != null) pStatement.setString(3, date);
				else pStatement.setNull(3, Types.DATE);
				if (countryID > 0) pStatement.setInt(4, countryID);
				else pStatement.setNull(4, Types.INTEGER);

				pStatement.execute();
				conn.commit();
				return labelID;
			} catch (SQLException e) {
				e.printStackTrace();
				return -1;
			}
		}
		return labelID;
	}

	/** Searches for a label by name and returns the labelID for the first one found.
	 *
	 * @param label Label name to search for
	 * @return the first labelID matching the label name. 0 if not present, or -1 if error encountered.
	 */
	public int getRecordLabelID(String label) {
		try {
			pStatement = conn.prepareStatement(
					"SELECT LabelID " +
					" FROM recordlabel" +
					" WHERE recordlabel.Name" +
					" LIKE ?" +
					" LIMIT 1;");
			pStatement.setString(1, label);
			ResultSet rs = pStatement.executeQuery();
			if (rs.next()) {
				int labelID = rs.getInt("LabelID");
				rs.close();
				return labelID;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}

	//private helper method for returning a random ID#
	private int getID(String s) {
		return rand.nextInt(Integer.MAX_VALUE);
	}

	//disconnects all DB resources if initialized
	public void disconnect() {
		System.out.print("closing db connection...");
		try {
			//close all DB resources
			if (conn != null) conn.close();
			if (pStatement != null) pStatement.close();
			if (statement != null) statement.close();
			if (conn != null) conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("closed!");
	}
}
