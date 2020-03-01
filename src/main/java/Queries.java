
package main.java;

import java.sql.*;
import java.util.Objects;
import java.util.Random;


public class Queries {
	private Connection conn;
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
		//create statement using try-with-resources block to ensure close regardless of success
		try (PreparedStatement pstmt = conn.prepareStatement(
				"SELECT ReleaseName, trim(LEADING ':' FROM trim(LEADING '0' FROM sec_to_time(Duration))) AS Duration, AlbumName, date(ReleaseDate) AS ReleaseDate, creator.CreatorID "
				+ "FROM album, audiofile, createdby, creator "
				+ "WHERE album.AlbumID = audioFile.AlbumID "
				+ "AND audiofile.TrackID = createdby.TrackID "
				+ "AND createdby.CreatorID = creator.CreatorID "
				+ "AND creator.Name = ? "
				+ "ORDER BY ReleaseDate ASC;")) //order results by release date of album
		{
			pstmt.setString(1, ctr);
			//make query
			try (ResultSet rs = pstmt.executeQuery()) {
				//check if results were found
				if(!rs.next()) {
					System.out.println ("No results found for " + ctr);
				} else {
					//display results
					System.out.println("From Creator: " + ctr + " (cid " + rs.getString("CreatorID") + ")");
					System.out.printf("%4s   %-20s   %5s   %-64s\n", "Year", "Album Name", "Drtn.", "Audio File Name");
					System.out.println("----------------------------------------------------------");
					while(rs.next()) {
						String duration = rs.getString("Duration");
						String aName = abbreviate(rs.getString("AlbumName"), 20);
						String releaseDate = abbreviate(nullable(rs.getString("ReleaseDate")), 4);
						System.out.printf("%4s │ %-20s │ %5s │ %-64s\n", releaseDate, aName, duration, rs.getString("ReleaseName"));
					}
					System.out.println(); //newline to separate results from next menu
				}
			}
		} catch (Exception exc){
			System.out.println("Error when searching for artist \"" + ctr + "\": " + exc.getMessage());
		}
	}

	/**
	 * querybyAudioTitle take in track name and prints track name, duration of track, creator name, album name, and release date
	 * @param title - title of audio file queried
	 */
	public void queryByAudioTitle(String title) {
		//create statement using try-with-resources block to ensure close regardless of success
		try (PreparedStatement pstmt = conn.prepareStatement(
				"SELECT ReleaseName, creator.Name, trim(LEADING ':' FROM trim(LEADING '0' FROM sec_to_time(Duration))) AS Duration, AlbumName, date(ReleaseDate) AS ReleaseDate, ExplicitRating\n" +
				" FROM album, audiofile" +
				" LEFT JOIN createdby" +
				" ON audiofile.TrackID = createdby.TrackID" +
				" LEFT JOIN creator" +
				" ON creator.CreatorID = createdby.CreatorID" +
				" WHERE album.AlbumID = audioFile.AlbumID" +
				" AND audiofile.ReleaseName = ?" +
				" ORDER BY ReleaseDate ASC;")) //if two songs have name order by release date
		{
			pstmt.setString(1, title);
			//make query
			try (ResultSet rs = pstmt.executeQuery()) {
				//check if results were found
				if(!rs.next()) {
					System.out.println ("No results found for " + title);
				} else {
					//display results
					System.out.printf("%-22s   %-18s   %-5s   %-20s   %-8s   %s\n", "Audio File Name:", "Creator:", "Drtn.", "Album Name:", "Explicit:", "Release Date:");
					System.out.println("\t---------------------------------------");
					do {
						System.out.printf("%-22s │ %-18s │ %-5s │ %-20s │ %-9s │ %s\n",
								abbreviate(rs.getString("ReleaseName"), 22),
								abbreviate(nullable(rs.getString("creator.Name")), 18),
								rs.getString("Duration"),
								abbreviate(rs.getString("AlbumName"), 20),
								(rs.getInt("ExplicitRating") == 0) ? "Clean" : "Explicit", //replace 0/1 rating with words
								nullable(rs.getString("ReleaseDate")));
					} while(rs.next());
				}
			}
		} catch (Exception exc){
			System.out.println("Error when searching for track \"" + title + "\": " + exc.getMessage());
		}
	}

	/**
	 * queryByAlbumTitle prints the album name, creator name, total duration of album, release date, label if applicable and
	 * country if applicable. It then prints track info for tracks in album
	 * @param title - title of album queried
	 */
	public void queryByAlbumTitle(String title) {
		//create statement using try-with-resources block to ensure close regardless of success
		try (PreparedStatement pstmt = conn.prepareStatement(
				"SELECT album.AlbumID, AlbumName, MediaType, date(ReleaseDate) AS ReleaseDate, recordlabel.Name AS Label, trim(LEADING ':' FROM trim(LEADING '0' FROM sec_to_time(SUM(Duration)))) AS Duration, count(audiofile.TrackID) AS Count" +
				" FROM album" +
				" LEFT JOIN recordlabel" +
				" ON album.LabelID = recordlabel.LabelID" +
				" LEFT JOIN audiofile" +
				" ON audiofile.AlbumID = album.AlbumID" +
				" WHERE album.AlbumName = ?" +
				" GROUP BY album.AlbumID" +
				" ORDER BY ReleaseDate DESC;"))
		{
			pstmt.setString(1, title);
			//make query
			try (ResultSet rs = pstmt.executeQuery()) {
				//check if results were found
				if(!rs.next()) {
					System.out.println ("No results found for " + title);
				} else {
					//print album info
					do {
						int count = rs.getInt("Count");
						String boxString = "  ";
						if (count > 0) boxString = "┌─";
						System.out.printf("Album: "+ boxString +"%-20s │ %7s │ %8s │ %s\n",
								rs.getString("AlbumName"),
								nullable(rs.getString("Duration")),
								rs.getString("MediaType"),
								nullable(rs.getString("Label")),
								nullable(rs.getString("ReleaseDate"))
						);

						//print tracks for each album, if any are present
						queryTracksByAlbumID(rs.getInt("AlbumID"), count);
					} while(rs.next());
				}
			}
		} catch (Exception exc){
			System.out.println("Error when searching for album \"" + title + "\": " + exc.getMessage());
		}
	}

	//called from any query that needs to print track info per album.
	private void queryTracksByAlbumID(int albumID, int count) {
		//create statement using try-with-resources block to ensure close regardless of success
		try (PreparedStatement pstmt = conn.prepareStatement(
				"SELECT ReleaseName AS Title, trim(LEADING ':' FROM trim(LEADING '0' FROM sec_to_time(Duration))) AS Duration, ExplicitRating AS Explicit, creator.Name AS Creator" +
				" FROM audiofile" +
				" LEFT JOIN createdby" +
				" ON createdby.TrackID = audiofile.TrackID" +
				" LEFT JOIN creator" +
				" ON createdby.CreatorID = creator.CreatorID" +
				" WHERE AlbumID = ?;"))
		{
			pstmt.setInt(1, albumID);
			try (ResultSet rs = pstmt.executeQuery()){
				if(!rs.next()) {
					System.out.println ("\tNo tracks found for album ID: " + albumID + "\n");
				} else {
					String boxShape = "├─";
					System.out.printf("       │ %-20s   %7s   %8s   %s\n", "      -Title-", "-Drtn-", "-Rating-", "-Creator-");
					int c = 0;
					do {
						if (++c == count) boxShape = "└─";
						String creator = abbreviate(nullable(rs.getString("Creator")), 20);
						String rating = (rs.getInt("Explicit") == 0) ? "Clean" : "Explicit"; //convert 0/1 to string
						String duration = rs.getString("Duration");
						String title = abbreviate(rs.getString("Title"), 20);
						System.out.printf("       " + boxShape + "%-20s │ %7s │ %8s │ %-20s\n", title, duration, rating, creator);
					} while (rs.next());
					System.out.println();
				}
			}
		} catch (SQLException e) {
			System.out.println("Error when searching for albumID \"" + albumID + "\": " + e.getMessage());
		}
	}

	public void queryByGenre(String gnr) {
		//create statement using try-with-resources block to ensure close regardless of success
		try (PreparedStatement pstmt = conn.prepareStatement(
				"SELECT Description, ReleaseName, creator.Name,  trim(LEADING ':' FROM trim(LEADING '0' FROM sec_to_time(Duration))) AS Duration, AlbumName, date(ReleaseDate) AS ReleaseDate "
				+ "FROM album, audiofile, createdby, creator, ingenre, genre "
				+ "WHERE album.AlbumID = audioFile.AlbumID "
				+ "AND audiofile.TrackID = createdby.TrackID "
				+ "AND createdby.CreatorID = creator.CreatorID "
				+ "AND ingenre.TrackID = audiofile.TrackID "
				+ "AND genre.GenreID = ingenre.GenreID "
				+ "AND genre.GenreID = ? "
				+ "ORDER BY ReleaseDate ASC;")) //order results by release date of album
			{
			pstmt.setString(1, gnr);
			//make query
			try (ResultSet rs = pstmt.executeQuery()) {
				//check if results were found
				if(!rs.next()) {
					System.out.println ("No results found for " + gnr);
				} else {
					//display results
					System.out.println("From Genre: " + gnr + "\t-" + rs.getString("Description"));
					System.out.println("\n\t---------------------------------------");
					System.out.printf("%-23s   %-20s   %-5s   %-20s   %s\n", "Audio File Name", "Creator", "Drtn.", "Album Name", "Release Date");
					//System.out.println("\nAudio File Name:\tCreator:\tDuration:\tAlbum Name:\tRelease Date:");
					System.out.println("\t---------------------------------------");
					do {
						System.out.printf("%-23s │ %-20s │ %-5s │ %-20s │ %s\n",
								abbreviate(rs.getString("ReleaseName"), 23),
								abbreviate(rs.getString("creator.Name"), 20),
								rs.getString("Duration"),
								abbreviate(rs.getString("AlbumName"), 20),
								rs.getString("ReleaseDate"));
					} while(rs.next());
				}
			}
		}
		catch (Exception exc){
			System.out.println("Error when searching for genre \"" + gnr + "\": " + exc.getMessage());
		}
	}

	public void queryByMediaType(String mediaType) {
		//create statement using try-with-resources block to ensure close regardless of success
		try (PreparedStatement pstmt = conn.prepareStatement(
					"SELECT a.AlbumID, AlbumName, date(ReleaseDate) AS ReleaseDate, r.Name AS Label, trim(LEADING ':' FROM trim(LEADING '0' FROM sec_to_time(SUM(Duration)))) AS Duration, count(af.TrackID) AS Count" +
					" FROM album a " +
					" LEFT JOIN recordlabel r" +
					" ON a.LabelID = r.LabelID" +
					" LEFT JOIN audiofile af" +
					" ON a.AlbumID = af.AlbumID" +
					" WHERE MediaType = ?" +
					" GROUP BY a.AlbumID;"))
		{
			pstmt.setString(1, mediaType);
			//make query
			try (ResultSet rs = pstmt.executeQuery()) {
				//check if results were found
				if(!rs.next()) {
					System.out.println ("No results found for media type: " + mediaType);
				} else {
					//print album info
					do {
						int count = rs.getInt("Count");
						String boxString = "  ";
						if (count > 0) boxString = "┌─";
						System.out.printf("%-5s: "+ boxString +"%-20s │ %7s │ %s │ %s\n",
								"Album",
								rs.getString("AlbumName"),
								nullable(rs.getString("Duration")),
								nullable(rs.getString("Label")),
								nullable(rs.getString("ReleaseDate"))
						);

						//print tracks for each album, if any are present
						queryTracksByAlbumID(rs.getInt("AlbumID"), count);
					} while(rs.next());
				}
			}
		}
		catch (Exception exc){
			System.out.println("Error when searching for media type \"" + mediaType + "\": " + exc.getMessage());
		}
	}


	/**
 	* Returns list of tracks with their artist based on user seeking explicit or not rating
 	* @param exp_num What kind of tracks to display. 1 for explicit tracks, or 0 for clean tracks.
 	*/
	public void getTracksByRating(int exp_num){
		//create statement using try-with-resources block to ensure close regardless of success
		//setup rs and p_stmt
		try (PreparedStatement p_stmt = conn.prepareStatement("SELECT audiofile.TrackID, audiofile.ReleaseName, audiofile.Duration, creator.Name AS Artist, creator.CreatorID" +
        	" FROM audiofile, createdby, creator" +
        	" WHERE audiofile.TrackID=createdby.TrackID" +
        	" AND createdby.CreatorID=creator.CreatorID" +
        	" AND audiofile.ExplicitRating=?" +
        	" ORDER BY creator.Name ASC;"))
		{
        	p_stmt.setInt(1, exp_num);
        	try (ResultSet rs = p_stmt.executeQuery()) {
				//check for empty/broken result
				if (!rs.next()) {
					System.out.println("Error: broken query or erroneous value passed!");
				} else {
					//produce result
					//System.out.println("TrackID:\tReleaseName:\tDuration:\tArtist:\tCreatorID:");
					System.out.printf("%15s   %41s   %5s   %-20s   %15s\n", "TrackID", "Track Name", "Drtn.", "Artist", "ArtistID");
					do {
						//System.out.println(rs.getInt(1) + "\t" + rs.getString(2) + "\t" + rs.getInt(3) + "\t" + rs.getString(4) + "\t" + rs.getInt(5));
						String tID = rs.getString("TrackId");
						String t = abbreviate(rs.getString("ReleaseName"), 40);
						String d = rs.getString("Duration");
						String a = abbreviate(rs.getString ("Artist"), 19);
						String aID = rs.getString("CreatorID");
						System.out.printf("%15s │ %-41s │ %5s │ %-20s │ %15s\n", tID, t, d, a, aID); 
					} while(rs.next());
				}
			}
    	} catch(Exception exc){
			System.out.println("Error when searching for rating \"" + exp_num + "\": " + exc.getMessage());
    	}
	}


	/**
	 * Gets lists of songs based on country.  Includes artist information.
     *
	 * @param country - country name
	 */
	public void getTracksByCountry(String country){
		//create statement using try-with-resources block to ensure close regardless of success
		//setup rs and p_stmt
		try (PreparedStatement p_stmt = conn.prepareStatement(
                "SELECT audiofile.TrackID, audiofile.ReleaseName, audiofile.Duration, creator.Name AS Artist, creator.CreatorID "
                + "FROM audiofile, createdby, creator, country "
                + "WHERE audiofile.TrackID=createdby.TrackID "
                + "AND createdby.CreatorID=creator.CreatorID "
                + "AND country.CountryID=audiofile.CountryID "
                + "AND country.Name=?;"))
		{
            p_stmt.setString(1, country);
            try (ResultSet rs = p_stmt.executeQuery()) {
				//check for empty/broken result
				if(!rs.next()) {
					System.out.println("No results found for country " + country);
				} else {
					//produce result
					System.out.printf("%15s   %-41s   %5s   %-20s   %15s\n", "TrackID", "Track Name", "Drtn.", "Artist", "ArtistID");
					do {
						//System.out.println(rs.getInt(1) + "\t" + rs.getString(2) + "\t" + rs.getInt(3) + "\t" + rs.getString(4) + "\t" + rs.getInt(5));
						String tID = rs.getString("TrackId");
						String t = abbreviate(rs.getString("ReleaseName"), 40);
						String d = rs.getString("Duration");
						String a = abbreviate(rs.getString ("Artist"), 19);
						String aID = rs.getString("CreatorID");
						System.out.printf("%15s │ %-41s │ %5s │ %-20s │ %15s\n", tID, t, d, a, aID);

					} while(rs.next());
				}
			}
        } catch(Exception exc){
			System.out.println("Error when searching for country \"" + country + "\": " + exc.getMessage());
        }
    }


    /**
     * Get list of tracks under specified record label.  Includes some artist info.
     * 
     * @param label_name Name of label to search for.
     */
    public void getTracksLabel(String label_name){
		//create statement using try-with-resources block to ensure close regardless of success
		try (PreparedStatement p_stmt = conn.prepareStatement(
                "SELECT AlbumName, date(ReleaseDate) AS ReleaseDate,trim(LEADING ':' FROM (trim(LEADING '0' FROM (sec_to_time(SUM(Duration)))))) AS Duration, MediaType, album.AlbumID AS AlbumID, recordlabel.Name AS Label, count(audiofile.TrackID) AS Count" +
				" FROM recordlabel, album, audiofile" +
				" WHERE recordlabel.Name = ?" +
				" AND recordlabel.LabelID = album.LabelID" +
				" AND album.AlbumID = audiofile.AlbumID" +
				" GROUP BY album.AlbumID;"))
		{
            p_stmt.setString(1, label_name);
			try (ResultSet rs = p_stmt.executeQuery()) {
				//check for empty/broken result
				if(!rs.next()) {
					System.out.println("No results found for label " + label_name);
				} else {
					//produce result
					do {
						System.out.printf("Album: ┌─%-20s   %7s   %8s   %s   %s\n",
								rs.getString("AlbumName"),
								nullable(rs.getString("Duration")),
								rs.getString("MediaType"),
								nullable(rs.getString("Label")),
								nullable(rs.getString("ReleaseDate"))
						);

						//print tracks for each album, if any are present
						queryTracksByAlbumID(rs.getInt("AlbumID"), rs.getInt("Count"));
					} while(rs.next());
				}
			}
        } catch(Exception exc) {
			System.out.println("Error when searching for label \"" + label_name + "\": " + exc.getMessage());
        }
    }

	/**
	 * Prints a list of all country names and the associated countryID
	 */
	public void printCountryCodes() {
		//create statement using try-with-resources block to ensure close regardless of success
    	try (Statement statement = conn.createStatement()) {
			try (ResultSet rs = statement.executeQuery("SELECT country.Name, CountryID FROM country")) {
				if (!rs.next()) {
					System.out.println("No countries found in database");
				} else {
					System.out.print("ID# | Country\n");
					do {
						System.out.printf("%3d | %s\n", rs.getInt("CountryID"), rs.getString("country.Name"));
					} while (rs.next());
				}
			}
		} catch (SQLException e) {
			System.out.println("Error when attempting to print country codes: " + e.getMessage());
		}
	}

	/**
	 * Get the average track duration for all tracks in a user specified album
	 *
	 * @param alb Name of album to get info for
	 */
	public void getAvgTrackDurationAlbum(String alb){
		//setup rs and p_stmt
		//create statement using try-with-resources block to ensure close regardless of success
		try (PreparedStatement p_stmt = conn.prepareStatement(
					"SELECT AVG(audiofile.Duration)" +
					" FROM audiofile, album WHERE audiofile.AlbumID=album.AlbumID" +
					" AND album.AlbumName='?';"))
		{
			p_stmt.setString(1, alb);

			try (ResultSet rs = p_stmt.executeQuery()) {
				if(rs.next()){
					System.out.println("The average track duration for album '" + alb +"' is: " + rs.getInt(1));
				} else {
					System.out.println("~~ERROR: Album '" + alb + "' returned no result ~~");
				}
			}

		} catch(Exception exc){
			exc.printStackTrace();
		}
	}


	/**
	 * Get total number of tracks in a user specified album
	 *
	 * @param alb Name of album to get info for
	 */
	public void numTracksInAlbum(String alb){
		//setup rs and p_stmt
		//create statement using try-with-resources block to ensure close regardless of success
		try (PreparedStatement p_stmt = conn.prepareStatement(
					"SELECT COUNT(audiofile.TrackID)" +
					" FROM audiofile, album WHERE audiofile.AlbumID=album.AlbumID" +
					" AND album.AlbumName='?';"))
		{
			p_stmt.setString(1, alb);
			try (ResultSet rs = p_stmt.executeQuery()) {
				if(rs.next()) {
					System.out.println("Total track count for '" + alb + "' is: " + rs.getInt(1));
				}
				else{
					System.out.println("~~ERROR: Album '" + alb + "' returned no result ~~");
				}
			}
		} catch(Exception exc){
			exc.printStackTrace();
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
		if (label != null) labelID = insertRecordLabel(label, null, 0);
		//create statement using try-with-resources block to ensure close regardless of success
		try (PreparedStatement pStatement = conn.prepareStatement(
					"INSERT INTO adb.album (AlbumID, AlbumName, MediaType, ReleaseDate, LabelID) " +
					" VALUES (?, ?, ?, ?, ?);"))
		{
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
			System.out.println("Error when inserting album \"" + albumName + "\": " + e.getMessage());
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
			//use try-with-resources block to ensure close regardless of success
			try (PreparedStatement pStatement = conn.prepareStatement(
						"INSERT INTO adb.recordlabel (LabelID, Name, FoundingDate, CountryID)" +
						" VALUES (?, ?, ?, ?);"))
			{
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
				System.out.println("Error when inserting label \"" + label + "\": " + e.getMessage());
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
		//use try-with-resources block to ensure close regardless of success
		try (PreparedStatement pStatement = conn.prepareStatement(
					"SELECT LabelID " +
					" FROM recordlabel" +
					" WHERE recordlabel.Name" +
					" LIKE ?" +
					" LIMIT 1;"))
		{
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
	 * Insert a new audio file into the db.
	 *
	 * @param name Name of the audio file
	 * @param rating Explicit rating of the file. 1 for explicit, 0 for clean.
	 * @param duration Duration of the file in seconds.
	 * @param countryID Country code for the file. Can be null.
	 * @return trackID File code for the inserted track.
	 */
	public int insertAudiofile(String name, Integer rating, Integer duration, Integer countryID, Integer albID){
		int trackID = getID(name);
		//use try-with-resources block to ensure close regardless of success
		try (PreparedStatement p_stmt = conn.prepareStatement("INSERT INTO adb.audiofile "
            + "(TrackID, ReleaseName, ExplicitRating, Duration, CountryID, AlbumID) "
            + " VALUES (?, ?, ?, ?, ?, ?);"))
		{
            //set vals
            p_stmt.setInt(1, trackID);
            p_stmt.setString(2, name);
			p_stmt.setInt(3, Objects.requireNonNullElse(rating, 0));
			//we can choose a different default here, but 0 could be bad
			p_stmt.setInt(4, Objects.requireNonNullElse(duration, 999));
			if (countryID == null) p_stmt.setNull(5, Types.INTEGER);
			else p_stmt.setInt(5, countryID);
            p_stmt.setInt(6, albID);

            //excute insert statement and commit result
            p_stmt.execute();
            conn.commit();
            System.out.println("New Track " + name + " added successfully with ID: " + trackID);

        } catch(SQLException sexc) {
			System.out.println("Error inserting track: " + sexc.getMessage());
            return -1;
    	}
    	return trackID;
	}

	/** Associates the specified genre with the specified track.
	 *
	 * @param tracKID Track code the genre should be associated with.
	 * @param Genre Name of genre to add to track.
	 * @return True if successful, false otherwise.
	 */
	public boolean addGenreToTrack(int tracKID, String Genre) {
		//use try-with-resources block to ensure close regardless of success
		try (PreparedStatement pStatement = conn.prepareStatement(
					"INSERT INTO adb.ingenre (TrackID, GenreID)" +
					" VALUES (?, ?);"))
		{
			pStatement.execute();
			conn.commit();
		} catch (SQLException e) {
			System.out.println("Error adding Genre " + Genre + " to trackID " + tracKID + ": " + e.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * insertCreator intakes the name of a new creator and gives them a unique creator ID
	 * @param name Name of creator.
	 */
	public int insertCreator(String name) {
		int creatorID = getID(name);
		//use try-with-resources block to ensure close regardless of success
		try (PreparedStatement pStatement = conn.prepareStatement(
				"INSERT INTO adb.creator (CreatorID, Name)" +
				" VALUES (?, ?);"))
		{
			//set values to insert
			pStatement.setInt(1, creatorID);
			pStatement.setString(2, name);
			pStatement.execute();
			conn.commit();
			pStatement.close();
			System.out.println("Successfully inserted new creator with ID: " + creatorID);
		} catch (SQLException e) {
			System.out.println("Error when inserting creator \"" + name + "\": " + e.getMessage());
			return -1;
		}
		return creatorID;
	}
	
	/**
	 * insert genre inserts a genre with name as ID and optional description
	 * @param genreID Name of genre.
	 * @param descrip Description of genre.
	 */
	public void insertGenre(String genreID, String descrip) {
		//use try-with-resources block to ensure close regardless of success
		try (PreparedStatement pStatement = conn.prepareStatement(
						"INSERT INTO genre (GenreID, Description)" +
						" VALUES (?, ?);"))
		{
			//insert values
			pStatement.setString(1, genreID);
			if (descrip != null) pStatement.setString(2, descrip);
			else pStatement.setNull(2, Types.VARCHAR);
			pStatement.execute();
			conn.commit();
			pStatement.close();
			System.out.println("Successfully inserted new genre: " + genreID);
		} catch(SQLIntegrityConstraintViolationException e) {
			System.out.println("Genre already exists in the database.");
		} catch (SQLException e) {
			System.out.println("Error when inserting genre \"" + genreID + "\": " + e.getMessage());
		}
	}

	/**
	 * insertCreator intakes the name of a new country and gives a unique ID
	 * @param name Name of country
	 */
	public int insertCountry(String name) {
		int countryID = getID(name);
		//use try-with-resources block to ensure close regardless of success
		try (PreparedStatement pStatement = conn.prepareStatement(
				"INSERT INTO adb.country (CountryID, Name)" +
				" VALUES (?, ?);"))
		{
			//set values to insert
			pStatement.setInt(1, countryID);
			pStatement.setString(2, name);
			pStatement.execute();
			conn.commit();
			pStatement.close();
			System.out.println("Successfully inserted new country with ID: " + countryID);
		} catch(SQLIntegrityConstraintViolationException e) {
			System.out.println("Country already exists in the database.");
		} catch (SQLException e) {
			System.out.println("Error when inserting country \"" + name + "\": " + e.getMessage());
			return -1;
		}
		return countryID;
	}
	
	/**
	 * updateGenre allows for changing the description of an existing genre
	 * @param genre name of genre to update
	 * @param descrip new description of genre
	 */
	public void updateGenre(String genre, String descrip) {
		//use try-with-resources block to ensure close regardless of success
		try (PreparedStatement pstmt = conn.prepareStatement(
				"SELECT GenreID, Description " +
				"FROM genre " +
				"WHERE GenreID = ?;"))
		{
			String oldDescrip = null;
			pstmt.setString(1, genre);
			try (ResultSet rs = pstmt.executeQuery()) {
				if(!rs.next()) {
					System.out.println("That genre does not exist yet.");
					return;
				}
				oldDescrip = rs.getString(2);
			}
			try (PreparedStatement pStatement = conn.prepareStatement(
					"UPDATE adb.genre "+ 
					"SET genre.Description = ? " +
					"WHERE GenreID = ?;"))
			{
				//set values to insert
				if (descrip != null) pStatement.setString(1, descrip);
				else pStatement.setNull(1, Types.VARCHAR);
				pStatement.setString(2, genre);
				pStatement.executeUpdate();
				conn.commit();
				pStatement.close();
				System.out.println("Successfully updated new genre description: " + genre);
				System.out.println("Description " + oldDescrip +" is now: " + descrip);
			}
		}catch(SQLException e) {
			System.out.println("Error when updating genre: " + genre + ": " + e.getMessage());
		} 
	}
	
	/**
	 * updateLabelCountry allows for changing the country of an existing label
	 * @param label name of label to update
	 * @param country new country of label
	 */
	public void updateLabelCountry(String label, Integer country) {
		//use try-with-resources block to ensure close regardless of success
		try (PreparedStatement pstmt = conn.prepareStatement(
				"SELECT LabelID, CountryID " +
				"FROM recordlabel " +
				"WHERE Name = ?;"))
		{
			String oldCountry = null;
			pstmt.setString(1, label);
			try (ResultSet rs = pstmt.executeQuery()) {
				if(!rs.next()) {
					System.out.println("That label does not exist yet.");
					return;
				}
				oldCountry = rs.getString(2);
			}
			try (PreparedStatement pStatement = conn.prepareStatement(
					"UPDATE adb.recordlabel "+ 
					"SET CountryID = ? " +
					"WHERE Name = ?;"))
			{
				//set values to insert
				//int cID = getCountryID(country);
				if (country != null) pStatement.setInt(1, country);
				else pStatement.setNull(1, Types.INTEGER);
				pStatement.setString(2, label);
				pStatement.executeUpdate();
				conn.commit();
				pStatement.close();
				System.out.println("Successfully updated new country for Record Label: " + label);
				System.out.println("CountryID " + oldCountry +" of " + label + " is now: " + country);
			}
		}catch(SQLException e) {
			System.out.println("Error when updating label: " + label + ": " + e.getMessage());
		} 
	}
	
	/**
	 * updateLabelDate allows for changing the date of an existing label
	 * @param label name of label to update
	 * @param country new country of label
	 */
	public void updateLabelDate(String label, String date) {
		//use try-with-resources block to ensure close regardless of success
		try (PreparedStatement pstmt = conn.prepareStatement(
				"SELECT LabelID, FoundingDate " +
				"FROM recordlabel " +
				"WHERE Name = ?;"))
		{
			String oldDate = null;
			pstmt.setString(1, label);
			try (ResultSet rs = pstmt.executeQuery()) {
				if(!rs.next()) {
					System.out.println("That label does not exist yet.");
					return;
				}
				oldDate = rs.getString(2);
			}
			try (PreparedStatement pStatement = conn.prepareStatement(
					"UPDATE adb.recordlabel "+ 
					"SET FoundingDate = ? " +
					"WHERE Name = ?;"))
			{
				//set values to insert
				//int cID = getCountryID(country);
				if (date != null) pStatement.setString(1, date);
				else pStatement.setNull(1, Types.DATE);
				pStatement.setString(2, label);
				pStatement.executeUpdate();
				conn.commit();
				pStatement.close();
				System.out.println("Successfully updated Founding Date for Record Label: " + label);
				System.out.println("Founding Date " + oldDate +" of " + label + " is now: " + date);
			}
		}catch(SQLException e) {
			System.out.println("Error when updating label: " + label + ": " + e.getMessage());
		} 
	}

	/**
	 * updateAlbumRD allows for changing the release date of an existing album
	 * @param album name of album to update
	 * @param date the updated release date
	 */
	public void updateAlbumRD(String album, String date) {
		//use try-with-resources block to ensure close regardless of success
		try (PreparedStatement pstmt = conn.prepareStatement(
				"SELECT AlbumID, ReleaseDate " +
				"FROM album " +
				"WHERE AlbumName = ?;"))
		{
			String oldDate = null;
			pstmt.setString(1, album);
			try (ResultSet rs = pstmt.executeQuery()) {
				if(!rs.next()) {
					System.out.println("That album does not exist yet.");
					return;
				}
				oldDate = rs.getString(2);
			}
			try (PreparedStatement pStatement = conn.prepareStatement(
					"UPDATE adb.album "+ 
					"SET ReleaseDate = ? " +
					"WHERE AlbumName = ?;"))
			{
				//set values to insert
				//int cID = getCountryID(country);
				if (date != null) pStatement.setString(1, date);
				else pStatement.setNull(1, Types.DATE);
				pStatement.setString(2, album);
				pStatement.executeUpdate();
				conn.commit();
				pStatement.close();
				System.out.println("Successfully updated release date for album: " + album);
				System.out.println("Founding Date " + oldDate +" of " + album + " is now: " + date);
			}
		}catch(SQLException e) {
			System.out.println("Error when updating label: " + album + ": " + e.getMessage());
		} 
	}

	/**
	 * updateAlbumRL allows for changing the record label of an existing album
	 * @param album name of album to be updated
	 * @param label name of new label for album
	 */
	public void updateAlbumRL(String album, String label) {
		//use try-with-resources block to ensure close regardless of success
		try (PreparedStatement pstmt = conn.prepareStatement(
				"SELECT AlbumID, LabelID " +
				"FROM album " +
				"WHERE AlbumName = ?;"))
		{
			String oldLabel = null;
			pstmt.setString(1, album);
			try (ResultSet rs = pstmt.executeQuery()) {
				if(!rs.next()) {
					System.out.println("That album does not exist yet.");
					return;
				}
				oldLabel = rs.getString(2);
			}
			try (PreparedStatement pStatement = conn.prepareStatement(
					"UPDATE adb.album "+ 
					"SET LabelID = ? " +
					"WHERE AlbumName = ?;"))
			{
				//set values to insert
				int rlID = 0;
				if (label != null) { 
					rlID = getRecordLabelID(label);
					if (rlID == 0 || rlID == -1) {
						System.out.println("There was an error finding the specified Record Label");
						return;
					}
					pStatement.setInt(1, rlID);
				}
				else pStatement.setNull(1, Types.INTEGER);
				pStatement.setString(2, album);
				pStatement.executeUpdate();
				conn.commit();
				pStatement.close();
				System.out.println("Successfully updated record label for album: " + album);
				System.out.println("Record LabelID " + oldLabel +" of " + album + " is now: " + rlID);
			}
		}catch(SQLException e) {
			System.out.println("Error when updating label: " + album + ": " + e.getMessage());
		} 
	}
	
	/**
	 * Allows user to change country associated with track
	 * 
	 * @param track_name
	 * @param c_name
	 */
	public void updateCountryIDaf(String track_name, String c_name){
	
		try(PreparedStatement p_stmt = conn.prepareStatement(
			"UPDATE adb.audiofile SET CountryID=? WHERE audiofile.ReleaseName=?;"
		)){
			int c_id = getCountryID(c_name);
			p_stmt.setInt(1, c_id);
			p_stmt.setString(2, track_name);
			p_stmt.executeUpdate();
			conn.commit();
			System.out.println("Successfully updated country for " + track_name + " to: " + c_name);
	
		}catch(Exception exc){
			exc.printStackTrace();
		}
	} 
	
	/**
	 * Helper method returns countryID given a coutnry name 
	 * 
	 * @param c_name
	 * @return
	 */
	public int getCountryID(String c_name){
		try(PreparedStatement p_stmt = conn.prepareStatement(
			"SELECT country.CountryID FROM country WHERE country.Name=?;"
		)){
			p_stmt.setString(1, c_name);
			try(ResultSet rs = p_stmt.executeQuery()){
				if(rs.next()){
					int c_id = rs.getInt(1);
					return c_id;
				}else{
					System.out.println("Error: returning unnamed countryID");
					return -1;
				}
			}
	
		}catch(Exception exc){
			exc.printStackTrace();
		}
		return -1;
	}
	
	public int deleteCreator(String creator){
		return 0;
	}
	
	public int deleteAlbum(String album) {
		try(PreparedStatement pStatement = conn.prepareStatement(
				"SELECT AlbumID" +
				"FROM album" +
				"WHERE AlbumName = ?;"))
		{
			pStatement.setString(1, album);
			try(ResultSet rs = pStatement.executeQuery()){
				if(!rs.next()) {
					return -1;
				}
				else {
					int albumID = rs.getInt(1);
					try(PreparedStatement pstmt = conn.prepareStatement(
							"SELECT ReleaseName" +
							"FROM audiofile" +
							"WHERE AlbumID = ?;"))
					{
						pstmt.setInt(1, albumID);
						ResultSet results = (pstmt.executeQuery());
						do{
							deleteTrack(results.getString("ReleaseName"));
						}while(results.next());
						pstmt.close();
					}
					try(PreparedStatement p_stmt = conn.prepareStatement(
							"DELETE" +
							"FROM album" +
							"WHERE AlbumID = ?;"))
					{
						p_stmt.setInt(1, albumID);
						p_stmt.executeUpdate();
						conn.commit();
						p_stmt.close();
					}
					pStatement.close();
					return 0;
				}
			}
		}
		catch(SQLException e) {
			return -1;
		}
	}
	
	public int deleteTrack(String audiofile) {
		try(PreparedStatement pStatement = conn.prepareStatement(
				"SELECT TrackID" +
				"FROM audiofile" +
				"WHERE ReleaseName = ?;"))
		{
			pStatement.setString(1, audiofile);
			try(ResultSet rs = pStatement.executeQuery()){
				if(!rs.next()) {
					return -1;
				}
				else {
					int trackID = rs.getInt(1);
					try(PreparedStatement pstmt = conn.prepareStatement(
							"DELETE" +
							"FROM ingenre" +
							"WHERE TrackID = ?;"))
					{
						pstmt.setInt(1, trackID);
						pstmt.executeUpdate();
						pstmt.close();
					}
					try(PreparedStatement p_stmt = conn.prepareStatement(
							"DELETE" +
							"FROM createdBy" +
							"WHERE TrackID = ?;"))
					{
						p_stmt.setInt(1, trackID);
						p_stmt.executeUpdate();
						p_stmt.close();
					}
					try(PreparedStatement preparedS = conn.prepareStatement(
							"DELETE" +
							"FROM audiofile" +
							"WHERE TrackID = ?;"))
					{
						preparedS.setInt(1, trackID);
						preparedS.executeUpdate();
						conn.commit();
						preparedS.close();
						return 0;
					}
				}
			}
		}
		catch(SQLException e) {
			return -1;
		}
	}
	
	public int deleteGenre(String genre) {
		try(PreparedStatement pStatement = conn.prepareStatement(
				"DELETE" +
				"FROM ingenre" +
				"WHERE GenreID = ?;"))
		{
			pStatement.setString(1, genre);
			pStatement.executeUpdate();
			try(PreparedStatement pstmt = conn.prepareStatement(
				"DELETE" +
				"FROM genre" +
				"WHERE GenreID = ?;"))
			{
				pstmt.setString(1, genre);
				pstmt.executeUpdate();
				conn.commit();
				pstmt.close();
			}
			pStatement.close();
			return 0;
		}
		catch(SQLException e) {
			return -1;
		}
	}
	
	public int deleteLabel(String label) {
		int labelID = getRecordLabelID(label);
		if(labelID == 0 || labelID == -1) {
			return -1;
		}
		try(PreparedStatement pStatement = conn.prepareStatement(
					"UPDATE album" +
					"SET LabelID = ?" +
					"WHERE LabelID = ?;"))
			{
				pStatement.setNull(1, Types.INTEGER);
				pStatement.setInt(2, labelID);
				pStatement.executeUpdate();
				
				try(PreparedStatement pstmt = conn.prepareStatement(
						"DELETE" +
						"FROM label" +
						"WHERE LabelID = ?;"))
				{
					pstmt.setInt(1, labelID);
					pstmt.executeUpdate();
					conn.commit();
					pstmt.close();
				}
				pStatement.close();
				return 0;
			}
		catch(SQLException e) {
			return -1;
		}
	}
	
	//private helper method for returning a random ID#
	private int getID(String s) {
		return rand.nextInt(Integer.MAX_VALUE);
	}

	//private helper method for abbreviating strings
	private String abbreviate(String s, int len) {
		String result = s.substring(0, Math.min(s.length(), len));
		if (s.length() > len && len > 4) result = result.substring(0, len - 3) + "...";
		return result;
	}

	//private helper method for casting null strings as blank
	private String nullable(String s) {
		if (s == null) return "-";
		return s;
	}
}
