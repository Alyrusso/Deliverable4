package main.java;

import java.sql.*;
import java.util.Objects;
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
			String stmt = "SELECT ReleaseName, trim(LEADING ':' FROM trim(LEADING '0' FROM sec_to_time(Duration))) AS Duration, AlbumName, date(ReleaseDate) AS ReleaseDate, creator.CreatorID "
					+ "FROM album, audiofile, createdby, creator "
					+ "WHERE album.AlbumID = audioFile.AlbumID "
					+ "AND audiofile.TrackID = createdby.TrackID "
					+ "AND createdby.CreatorID = creator.CreatorID "
					+ "AND creator.Name = ? "
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
				System.out.println("From Creator: " + ctr + " (cid " + rs.getString("CreatorID") + ")");
				System.out.printf("%4s   %-20s   %5s   %-64s\n", "Year", "Album Name", "Drtn.", "Audio File Name");
				System.out.println("----------------------------------------------------------");
				while(rs.next()) {
					String duration = rs.getString("Duration");
					String aName = abbreviate(rs.getString("AlbumName"), 20);
					String releaseDate = abbreviate(nullable(rs.getString("ReleaseDate")), 4);
					System.out.printf("%4s | %-20s | %5s | %-64s\n", releaseDate, aName, duration, rs.getString("ReleaseName"));
				}
				System.out.println(); //newline to separate results from next menu
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
			String stmt = "SELECT ReleaseName, Duration, creator.Name, AlbumName, ReleaseDate, ExplicitRating "
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
			String stmt = "SELECT album.AlbumID, AlbumName, MediaType, date(ReleaseDate) AS ReleaseDate, recordlabel.Name AS Label, sec_to_time(SUM(Duration)) AS Duration" +
					" FROM album" +
					" LEFT JOIN recordlabel" +
					" ON album.LabelID = recordlabel.LabelID" +
					" LEFT JOIN audiofile" +
					" ON audiofile.AlbumID = album.AlbumID" +
					" WHERE album.AlbumName = ?" +
					" GROUP BY album.AlbumID" +
					" ORDER BY ReleaseDate DESC";
			PreparedStatement pstmt = conn.prepareStatement(stmt);
			pstmt.setString(1, title);
			//make query
			ResultSet rs = pstmt.executeQuery();
			//check if results were found
			if(rs.next() == false) {
				System.out.println ("No results found for " + title);
			} else {
				//display results
				//System.out.printf("%8s   %-20s   %-10s   %-6s   %-20s\n", "Duration", "Album Name", "Released", "Media Type", "Label Name");
				//print album info
				do {
					System.out.printf("%-5s | %8s | %8s | %s | %s\n",
							rs.getString("MediaType"),
							rs.getString("AlbumName"),
							nullable(rs.getString("Duration")),
							nullable(rs.getString("Label")),
							nullable(rs.getString("ReleaseDate"))
					);

					//print tracks for each album, if any are present
					queryTracksByAlbumID(rs.getInt("AlbumID"));
				} while(rs.next());
			}
		}
		catch (Exception exc){
			exc.printStackTrace();
		}
	}

	//called from any query that needs to print track info per album.
	private void queryTracksByAlbumID(int albumID) {
		PreparedStatement pstmt = null;
		try {
			//create statement
			pstmt = conn.prepareStatement(
					"SELECT ReleaseName AS Title, trim(LEADING ':' FROM trim(LEADING '0' FROM sec_to_time(Duration))) AS Duration, ExplicitRating AS Explicit"
					+ " FROM audiofile"
					+ " WHERE AlbumID = ?"
			);
			pstmt.setInt(1, albumID);
			ResultSet rs = pstmt.executeQuery();

			if(!rs.next()) {
				System.out.println ("\tNo tracks found for album ID: " + albumID + "\n");
			} else {
				System.out.printf("\t%-20s   %8s   %s\n", "Title", "Duration", "Rating");
				System.out.println("\t---------------------------------------");
				do {
					String rating = (rs.getInt("Explicit") == 0) ? "Clean" : "Explicit"; //convert 0/1 to string
					String duration = rs.getString("Duration");
					String title = abbreviate(rs.getString("Title"), 20);
					System.out.printf("\t%-20s | %8s | %s\n", title, duration, rating);
				} while (rs.next());
				System.out.println();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
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

	public void queryByMediaType(String mediaType) {
		try{
			//create statement
			PreparedStatement pstmt = conn.prepareStatement(
					"SELECT a.AlbumID, AlbumName, date(ReleaseDate) AS ReleaseDate, r.Name AS Label, trim(LEADING ':' FROM trim(LEADING '0' FROM sec_to_time(SUM(Duration)))) AS Duration" +
					" FROM album a\n" +
					" LEFT JOIN recordlabel r" +
					" ON a.LabelID = r.LabelID" +
					" LEFT JOIN audiofile af" +
					" ON a.AlbumID = af.AlbumID" +
					" WHERE MediaType = ?" +
					" GROUP BY a.AlbumID;");
			pstmt.setString(1, mediaType);
			//make query
			ResultSet rs = pstmt.executeQuery();
			//check if results were found
			if(rs.next() == false) {
				System.out.println ("No results found for media type: " + mediaType);
			} else {
				//display results
				//System.out.printf("%8s   %-20s   %-10s   %-6s   %-20s\n", "Duration", "Album Name", "Released", "Media Type", "Label Name");
				//print album info
				do {
					System.out.printf("%-5s: %8s | %s | %s | %s\n",
							"Album",
							rs.getString("AlbumName"),
							nullable(rs.getString("Duration")),
							nullable(rs.getString("Label")),
							nullable(rs.getString("ReleaseDate"))
					);

					//print tracks for each album, if any are present
					queryTracksByAlbumID(rs.getInt("AlbumID"));
				} while(rs.next());
			}
		}
		catch (Exception exc){
			exc.printStackTrace();
		}
	}


	/**
 	* Returns list of tracks with their artist based on user seeking explicit or not rating
 	* @param exp_num
 	*/
	public void getTracksByRating(int exp_num){
    	ResultSet rs = null;
    	PreparedStatement p_stmt = null;

    	try{
        	//setup rs and p_stmt
        	p_stmt = conn.prepareStatement("SELECT audiofile.TrackID, audiofile.ReleaseName, audiofile.Duration, creator.Name AS Artist, creator.CreatorID" +
        	" FROM audiofile, createdby, creator" +
        	" WHERE audiofile.TrackID=createdby.TrackID" +
        	" AND createdby.CreatorID=creator.CreatorID" +
        	" AND audiofile.ExplicitRating=?" +
        	" ORDER BY creator.Name ASC;");
        	p_stmt.setInt(1, exp_num);
        	rs = p_stmt.executeQuery();

        	//check for empty/broken result
        	if(rs.next() == false){
            	System.out.println("Error: broken query or erroneous value passed!");
        	}

        	//produce result
        	else{
            	System.out.println("TrackID:\tReleaseName:\tDuration:\tArtist:\tCreatorID:");
            	do{
                	System.out.println(rs.getInt(1) + "\t" + rs.getString(2) + "\t" + rs.getInt(3) + "\t" + rs.getString(4) + "\t" + rs.getInt(5));

            	}while(rs.next());

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
	 * Gets lists of songs based on country.  Includes artist information.
     *
	 * @param country - country name
	 */
	public void getTracksByCountry(String country){
        ResultSet rs = null;
        PreparedStatement p_stmt = null;

        try{
            //setup rs and p_stmt
            p_stmt = conn.prepareStatement(
                "SELECT audiofile.TrackID, audiofile.ReleaseName, audiofile.Duration, creator.Name AS Artist, creator.CreatorID "
                + "FROM audiofile, createdby, creator, country "
                + "WHERE audiofile.TrackID=createdby.TrackID "
                + "AND createdby.CreatorID=creator.CreatorID "
                + "AND country.CountryID=audiofile.CountryID "
                + "AND country.Name=?;");
            p_stmt.setString(1, country);
            rs = p_stmt.executeQuery();

            //check for empty/broken result
            if(rs.next() == false){
                System.out.println("Error: broken query or erroneous value passed!");
            }

            //produce result
            else{
                System.out.println("TrackID:\tReleaseName:\tDuration:\tArtist:\tCreatorID:");
                do{
                    System.out.println(rs.getInt(1) + "\t" + rs.getString(2) + "\t" + rs.getInt(3) + "\t" + rs.getString(4) + "\t" + rs.getInt(5));

                }while(rs.next());

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
     * Get list of tracks under specified record label.  Includes some artist info.
     * 
     * @label_name
     */
    public void getTracksLabel(String label_name){
        ResultSet rs = null;
        PreparedStatement p_stmt = null;

        try{
            //setup rs and p_stmt
            p_stmt = conn.prepareStatement(
                "SELECT audiofile.TrackID, audiofile.ReleaseName, audiofile.Duration, creator.Name AS Artist, creator.CreatorID "
                + "FROM audiofile, createdby, creator, recordlabel, album "
                + "WHERE audiofile.TrackID=createdby.TrackID "
                + "AND createdby.CreatorID=creator.CreatorID "
                + "AND album.AlbumID=audiofile.AlbumID "
                + "AND recordlabel.LabelID=album.LabelID "
                + "AND recordlabel.Name=?;");
            p_stmt.setString(1, label_name);
            rs = p_stmt.executeQuery();

            //check for empty/broken result
            if(rs.next() == false){
                System.out.println("Error: broken query or erroneus value passed!");
            }

            //produce result
            else{
                System.out.println("TrackID:\tReleaseName:\tDuration:\tArtist:\tCreatorID:");
                do{
                    System.out.println(rs.getInt(1) + "\t" + rs.getString(2) + "\t" + rs.getInt(3) + "\t" + rs.getString(4) + "\t" + rs.getInt(5));

                }while(rs.next());

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
	 */
	public void numTracksInAlbum(String alb){
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
	public int insertAlbum(String albumName, String date, String label, String mediaType) {
		int albumID = -1;
		int labelID = 0;
		if (label != null) labelID = insertRecordLabel(label, null, 0); //
		try {
			pStatement = conn.prepareStatement(
					"INSERT INTO adb.album (AlbumID, AlbumName, MediaType, ReleaseDate, LabelID) " +
					" VALUES (?, ?, ?, ?, ?);");
			albumID = getID(albumName);
			pStatement.setInt(1, albumID);
			pStatement.setString(2, albumName);
			pStatement.setString(3, mediaType);
			if (date != null) pStatement.setString(4, date);
			else pStatement.setNull(4, Types.DATE);
			if (labelID > 0) pStatement.setInt(5, labelID);
			else pStatement.setNull(5, Types.INTEGER);

			pStatement.execute();
			conn.commit();
			System.out.println("Successfully inserted new album with ID: " + albumID);
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
				System.out.println("Successfully inserted new record label with ID: " + labelID);
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


	/**
	 * Insert a new audiofile into the db.
	 *
	 * @param name
	 * @param rating
	 * @param duration
	 * @param countryID
	 * @return trackID
	 */
	public int insertAudiofile(String name, Integer rating, Integer duration, Integer countryID, Integer albID){

		int trackID = getID(name);
		try{
			PreparedStatement p_stmt = conn.prepareStatement("INSERT INTO adb.audiofile "
            + "(TrackID, ReleaseName, ExplicitRating, Duration, CountryID, AlbumID) "
            + " VALUES (?, ?, ?, ?, ?, ?);");

            //set vals
            p_stmt.setInt(1, trackID);
            p_stmt.setString(2, name);
			p_stmt.setInt(3, Objects.requireNonNullElse(rating, 0));
			//we can choose a different default here, but 0 could be bad
			p_stmt.setInt(4, Objects.requireNonNullElse(duration, 999));
			if (countryID == null) p_stmt.setNull(5, Types.INTEGER);
			else p_stmt.setInt(5, countryID);
            p_stmt.setInt(6, albID);

            //exc
            p_stmt.execute();
            conn.commit();
            System.out.println("New Track " + name + " added successfully with ID: " + trackID);

        } catch(SQLException sexc){
			System.out.println("Error inserting track: " + sexc.getMessage());
            //sexc.printStackTrace();
            return -1;
    	}
    	return trackID;
	}


	//private helper method for returning a random ID#
	private int getID(String s) {
		return rand.nextInt(Integer.MAX_VALUE);
	}

	//private helper method for abbreviating strings
	private String abbreviate(String s, int len) {
		return s.substring(0, Math.min(s.length(), len));
	}

	//private helper method for casting null strings as blank
	private String nullable(String s) {
		if (s == null) return "-";
		return s;
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
