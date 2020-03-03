
package main.java;

import java.sql.*;
import java.util.*;


public class Queries {
	private Connection conn;
	private Random rand;
	private Scanner in;

	public Queries(Connection c, Scanner in) {
		this.conn = c;
		this.in = in;
		this.rand = new Random();
	}
	
	/**
	 * queryByCreator prints the track name, track duration, album name, and release date of all audio files by creator queried
	 * @param ctr -creator name
	 */
	public int queryByCreator(String ctr, boolean printIDs) {
		int count = 0;
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
					 do {
					 	count++;
						String duration = rs.getString("Duration");
						String aName = abbreviate(rs.getString("AlbumName"), 20);
						String releaseDate = abbreviate(nullable(rs.getString("ReleaseDate")), 4);
						String title = abbreviate(rs.getString("ReleaseName"), 20);
						System.out.printf("%4s │ %-20s │ %5s │ %-20s", releaseDate, aName, duration, title);
						if (printIDs) System.out.printf(" | ID: %s\n", rs.getInt("creator.CreatorID"));
						else System.out.println();
					} while(rs.next());
					System.out.println(); //newline to separate results from next menu
				}
			}
		} catch (Exception exc){
			System.out.println("Error when searching for artist \"" + ctr + "\": " + exc.getMessage());
		}
		return count;
	}

	/**
	 * querybyAudioTitle take in track name and prints track name, duration of track, creator name, album name, and release date
	 * @param title - title of audio file queried
	 */
	public int queryByAudioTitle(String title, boolean printIDs) {
		int count = 0;
		//create statement using try-with-resources block to ensure close regardless of success
		try (PreparedStatement pstmt = conn.prepareStatement(
				"SELECT ReleaseName, creator.Name, trim(LEADING ':' FROM trim(LEADING '0' FROM sec_to_time(Duration))) AS Duration, AlbumName, date(ReleaseDate) AS ReleaseDate, ExplicitRating, audiofile.TrackID" +
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
					System.out.printf("%-22s   %-18s   %-5s   %-20s   %-8s   %10s\n", "Audio File Name:", "Creator:", "Drtn.", "Album Name:", "Explicit:", "Released:");
					System.out.println("\t---------------------------------------");
					do {
						count++;
						System.out.printf("%-22s │ %-18s │ %-5s │ %-20s │ %-9s │ %10s",
								abbreviate(rs.getString("ReleaseName"), 22),
								abbreviate(nullable(rs.getString("creator.Name")), 18),
								rs.getString("Duration"),
								abbreviate(rs.getString("AlbumName"), 20),
								(rs.getInt("ExplicitRating") == 0) ? "Clean" : "Explicit", //replace 0/1 rating with words
								nullable(rs.getString("ReleaseDate")));
						if (printIDs) System.out.printf(" | ID: %s\n", rs.getInt("audiofile.TrackID"));
						else System.out.println();
					} while(rs.next());
				}
			}
		} catch (Exception exc){
			System.out.println("Error when searching for track \"" + title + "\": " + exc.getMessage());
		}
		return count;
	}

	/**
	 * queryByAlbumTitle prints the album name, creator name, total duration of album, release date, label if applicable and
	 * country if applicable. It then prints track info for tracks in album
	 * @param title - title of album queried
	 * @param printTracks Whether the query should print a list of tracks.
	 * @return A map of index+albumID pairs
	 */
	public Map<Integer, Integer> queryByAlbumTitle(String title, boolean printTracks) {
		Map<Integer, Integer> results = null;
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
					results = new HashMap<>();
					int i = 0;
					do {
						i++;

						int albumCount = rs.getInt("Count");
						int albumID = rs.getInt("album.AlbumID");
						results.put(i, albumID);
						String prefix = (printTracks) ? "Album: " : "  " + i + ": ";
						String boxString = "  ";
						if (albumCount > 0 && printTracks) boxString = "┌─";

						System.out.printf("%-7s" + boxString + "%-20s │ %7s │ %8s │ %10s | %s\n",
								prefix,
								rs.getString("AlbumName"),
								nullable(rs.getString("Duration")),
								rs.getString("MediaType"),
								nullable(rs.getString("ReleaseDate")),
								nullable(rs.getString("Label"))
						);

						//print tracks for each album, if any are present
						if (printTracks) queryTracksByAlbumID(rs.getInt("AlbumID"), albumCount);
					} while(rs.next());
				}
			}
		} catch (Exception exc){
			System.out.println("Error when searching for album \"" + title + "\": " + exc.getMessage());
		}
		return results;
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
					System.out.println ("         No tracks found for album ID: " + albumID + "\n");
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
						System.out.printf("       %2s%-20s │ %7s │ %8s │ %-20s\n", boxShape, title, duration, rating, creator);
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
								nullable(rs.getString("ReleaseDate")));
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
                "SELECT af.ReleaseName, year(al.ReleaseDate) AS Year, trim(LEADING ':' FROM (trim(LEADING '0' FROM (sec_to_time(Duration))))) AS Duration, cr.Name AS Artist, al.AlbumName" +
				" FROM audiofile AS af" +
				" LEFT JOIN createdby AS cb" +
				" ON cb.TrackID = af.TrackID" +
				" LEFT JOIN creator AS cr" +
				" ON cb.CreatorID = cr.CreatorID" +
				" LEFT JOIN country AS co" +
				" ON af.CountryID = co.CountryID" +
				" LEFT JOIN album AS al" +
				" ON al.AlbumID = af.AlbumID" +
				" WHERE co.Name= ?" +
				" ORDER BY Artist DESC, AlbumName DESC;"))
		{
            p_stmt.setString(1, country);
            try (ResultSet rs = p_stmt.executeQuery()) {
				//check for empty/broken result
				if(!rs.next()) {
					System.out.println("No results found for country " + country);
				} else {
					//produce result
					System.out.printf("%4s   %-20s   %5s   %-20s   %-20s\n", "Year", "Track Name", "Drtn.", "Creator", "Album");
					do {
						System.out.printf("%4s | %-20s | %5s | %-20s | %-20s\n",
							nullable(rs.getString("Year")),
							abbreviate(rs.getString("ReleaseName"), 20),
							rs.getString("Duration"),
							abbreviate(rs.getString ("Artist"), 20),
							abbreviate(rs.getString("AlbumName"), 20)
						);
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
                "SELECT AlbumName, date(ReleaseDate) AS ReleaseDate, MediaType, a.AlbumID AS AlbumID, r.Name AS Label, count(af.TrackID) AS Count, trim(LEADING ':' FROM (trim(LEADING '0' FROM (sec_to_time(SUM(Duration)))))) AS Duration\n" +
				" FROM recordlabel AS r" +
				" JOIN album AS a" +
				" LEFT JOIN audiofile AS af" +
				" ON af.AlbumID = a.AlbumID" +
				" WHERE r.Name = ?" +
				" AND r.LabelID = a.LabelID" +
				" GROUP BY a.AlbumID;"))
		{
            p_stmt.setString(1, label_name);
			try (ResultSet rs = p_stmt.executeQuery()) {
				//check for empty/broken result
				if(!rs.next()) {
					System.out.println("No albums found under label " + label_name);
				} else {
					//produce result
					do {
						int count = rs.getInt("Count");
						String boxString = (count > 0) ? "┌─" : "  ";
						System.out.printf("Album: %2s%-20s | %7s | %8s | %10s\n",
								boxString,
								rs.getString("AlbumName"),
								nullable(rs.getString("Duration")),
								rs.getString("MediaType"),
								nullable(rs.getString("ReleaseDate"))
						);

						//print tracks for each album, if any are present
						queryTracksByAlbumID(rs.getInt("AlbumID"), count);
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
			System.out.println("Error getting average track duration: " + exc.getMessage());
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
			System.out.println("Error getting track count for album " + alb + ": " + exc.getMessage());
		}
	}


	/**
	 * Returns a random list of tracks of size given by user.
	 * 
	 * @param list_size Maximum number of tracks to print
	 */
	public void getRandomTracks(int list_size){
		//setup rs and p_stmt
		//create statement using try-with-resources block to ensure close regardless of success
		try (PreparedStatement p_stmt = conn.prepareStatement(
					"SELECT audiofile.TrackID, audiofile.ReleaseName, audiofile.Duration, creator.Name AS Artist, creator.CreatorID"
					+ " FROM audiofile, createdby, creator"
					+ " WHERE audiofile.TrackID=createdby.TrackID"
					+ " AND createdby.CreatorID=creator.CreatorID"
					+ " ORDER BY RAND()"
					+ " LIMIT ?"
					))
		{
			
			p_stmt.setInt(1, list_size);
	
			try (ResultSet rs = p_stmt.executeQuery()) {
				//check for empty/broken result
				if (!rs.next()) {
					System.out.println("Error: broken query or erroneous value passed!");
				} else {
					//produce result
					//System.out.println("TrackID:\tReleaseName:\tDuration:\tArtist:\tCreatorID:");
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
	
		} catch(Exception exc) {
			System.out.println("Error getting random tracks: " + exc.getMessage());
		}
	}


	/** Inserts an album into the database.
	 * @param albumName Name of the album to insert
	 * @param date  Date the album came out. Format as a number like "20171231". Can be null.
	 * @param label Name of the record label. Inserts label name to DB if not already present. Can be null.
	 * @return the albumID of the inserted album. -1 if insert failed.
	 */
	public int insertAlbum(String albumName, String date, String label, String mediaType) {
		int albumID;
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
			albumID = -1;
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
				labelID = -1;
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
		int labelID;
		try (PreparedStatement pStatement = conn.prepareStatement(
					"SELECT LabelID " +
					" FROM recordlabel" +
					" WHERE recordlabel.Name" +
					" LIKE ?" +
					" LIMIT 1;"))
		{
			pStatement.setString(1, label);
			ResultSet rs = pStatement.executeQuery();
			if (rs.next()) labelID = rs.getInt("LabelID");
			else labelID = 0;
		} catch (SQLException e) {
			System.out.println("Error getting record label ID: " + e.getMessage());
			labelID = -1;
		}
		return labelID;
	}

	/**
	 * Insert a new audio file into the db.
	 *
	 * @param name Name of the audio file
	 * @param rating Explicit rating of the file. 1 for explicit, 0 for clean.
	 * @param duration Duration of the file in seconds.
	 * @param countryID Country code for the file. Can be null.
	 * @param albID The ID for the album this track belongs in.
	 * @param creator The name of the creator of this track.
	 * @return trackID File code for the inserted track.
	 */
	public int insertAudiofile(String name, Integer rating, Integer duration, Integer countryID, Integer albID, String creator){
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

            if (creator != null && creator.length() > 0) {
				int crtID = 0;
				//insert trackID and creatorID into createdby table
				try (PreparedStatement pstmt = conn.prepareStatement(
						"SELECT CreatorID " +
								"FROM creator " +
								"WHERE Name = ?;")){
					pstmt.setString(1, creator);
					try (ResultSet rs = pstmt.executeQuery()) {
						if(!rs.next()) {
							System.out.print("That creator was not found. Would you like to add new creator: " + creator + "? (q to quit, or y to add new creator: ");
							String answer = in.nextLine();
							if(answer.equals("y"))
								crtID = insertCreator(creator);
							else
								trackID = -1;
						}
						else
							crtID = rs.getInt(1);
					}
				}
				try (PreparedStatement pstmt2 = conn.prepareStatement("INSERT INTO adb.createdby "
						+ "(TrackID, CreatorID) "
						+ " VALUES (?, ?);")){
					pstmt2.setInt(1, trackID);
					pstmt2.setInt(2, crtID);
					//execute
					pstmt2.execute();
					conn.commit();
				}
			}
            System.out.println("New Track " + name + " added successfully with ID: " + trackID);

        } catch(SQLException sexc) {
			System.out.println("Error inserting track: " + sexc.getMessage());
            trackID = -1;
    	}
    	return trackID;
	}

	/** Associates the specified genre with the specified track.
	 *
	 * @param trackID Track code the genre should be associated with.
	 * @param genre Name of genre to add to track.
	 * @return True if successful, false otherwise.
	 */
	public boolean addGenreToTrack(int trackID, String genre) {
		//use try-with-resources block to ensure close regardless of success
		boolean success;
		try (PreparedStatement pStatement = conn.prepareStatement(
					"INSERT INTO adb.ingenre (TrackID, GenreID)" +
					" VALUES (?, ?);"))
		{
			pStatement.setInt(1, trackID);
			pStatement.setString(2, genre);
			pStatement.execute();
			conn.commit();
			success = true;
		} catch (SQLException e) {
			System.out.println("Error adding Genre " + genre + " to trackID " + trackID + ": " + e.getMessage());
			success = false;
		}
		return success;
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
			System.out.println("Successfully inserted new creator with ID: " + creatorID);
		} catch (SQLException e) {
			System.out.println("Error when inserting creator \"" + name + "\": " + e.getMessage());
			creatorID = -1;
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
			System.out.println("Successfully inserted new country with ID: " + countryID);
		} catch(SQLIntegrityConstraintViolationException e) {
			System.out.println("Country already exists in the database.");
		} catch (SQLException e) {
			System.out.println("Error when inserting country \"" + name + "\": " + e.getMessage());
			countryID = -1;
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
			String oldDescrip;
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
			String oldCountry;
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
				System.out.println("Successfully updated new country for Record Label: " + label);
				System.out.println("CountryID " + oldCountry +" of " + label + " is now: " + country);
			}
		}catch(SQLException e) {
			System.out.println("Error when updating label: " + label + ": " + e.getMessage());
		} 
	}
	
	/**
	 * updateLabelDate allows for changing the founding date of an existing label
	 * @param label name of label to update
	 * @param date new founding date of label
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
				if (date != null) pStatement.setString(1, date);
				else pStatement.setNull(1, Types.DATE);
				pStatement.setString(2, label);
				pStatement.executeUpdate();
				conn.commit();
				System.out.println("Success. Founding date of " + label +" is now: " + date);
			}
		}catch(SQLException e) {
			System.out.println("Error when updating label: " + label + ": " + e.getMessage());
		} 
	}

	/**
	 * updateAlbumRD allows for changing the release date of an existing album
	 * @param albumID the ID of the album to update
	 * @param date the updated release date
	 */
	public void updateAlbumRD(int albumID, String date) {
		//use try-with-resources block to ensure close regardless of success
		try (PreparedStatement pstmt = conn.prepareStatement(
				"SELECT ReleaseDate, AlbumName " +
				" FROM album " +
				" WHERE AlbumID = ?;"))
		{
			String oldDate = null;
			String album;
			pstmt.setInt(1, albumID);
			try (ResultSet rs = pstmt.executeQuery()) {
				if(!rs.next()) {
					System.out.println("That album does not exist yet.");
					return;
				}
				oldDate = rs.getString("ReleaseDate");
				album = rs.getString("AlbumName");
			}
			try (PreparedStatement pStatement = conn.prepareStatement(
					"UPDATE adb.album "+ 
					" SET ReleaseDate = ? " +
					" WHERE AlbumID = ?;"))
			{
				//set values to insert
				if (date != null) pStatement.setString(1, date);
				else pStatement.setNull(1, Types.DATE);
				pStatement.setInt(2, albumID);
				pStatement.executeUpdate();
				conn.commit();
				System.out.println("Success. Release date of " + album +" is now: " + date);
			}
		}catch(SQLException e) {
			System.out.println("Error when updating release date for albumID " + albumID + ": " + e.getMessage());
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
			String oldLabel;
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
				if (label != null) { 
					int rlID = getRecordLabelID(label);
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
				System.out.println("Success. Record LabelID (" + oldLabel +") of " + album + " is now: " + label);
			}
		}catch(SQLException e) {
			System.out.println("Error when updating label: " + album + ": " + e.getMessage());
		} 
	}
	
	/**
	 * Allows user to change country associated with track
	 * 
	 * @param track_name Name of track to update
	 * @param c_name New country name
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
			System.out.println("Error updating country " + c_name + "'s ID: " + exc.getMessage());
		}
	} 
	
	/**
	 * Helper method returns countryID given a country name
	 * 
	 * @param c_name Country name to find ID for.
	 * @return ID for requested country, or -1 if not present.
	 */
	public int getCountryID(String c_name){
		int result = 0;
		try(PreparedStatement p_stmt = conn.prepareStatement(
			"SELECT country.CountryID FROM country WHERE country.Name=?;"
		)) {
			p_stmt.setString(1, c_name);
			try (ResultSet rs = p_stmt.executeQuery()) {
				if (rs.next()) result = rs.getInt(1);
				else result = -1;
			}
		} catch(Exception exc) {
			System.out.println("Error getting country ID for " + c_name + ": " + exc.getMessage());
		}
		return result;
	}
	
	/**
	 * Deletes Creator and all audio files by the creator
	 * 
	 * @param creatorID the ID of the creator to delete
	 * @return Number of creators deleted (1), or -1 if error occurs.
	 */
	public int deleteCreator(int creatorID) {
		int count;
		try(PreparedStatement p_stmt = conn.prepareStatement(
				"DELETE" +
				" FROM creator" +
				" WHERE CreatorID = ?;"))
		{
			p_stmt.setInt(1, creatorID);
			count = p_stmt.executeUpdate();
			conn.commit();
		} catch(SQLException e){
			System.out.println("Error deleting creatorID " + creatorID + ": " + e.getMessage());
			count = -1;
		}
		return count;
	}
	
	/**
	 * Deletes album and all audiofiles on the album
	 * 
	 * @param albumID ID of the album to delete
	 * @return Number of items deleted (0/1), or -1 if an error occured.
	 */
	public int deleteAlbum(int albumID) {
		int items;
		try (PreparedStatement p_stmt = conn.prepareStatement(
				"DELETE" +
				" FROM album" +
				" WHERE AlbumID = ?;")) {
			p_stmt.setInt(1, albumID);
			items = p_stmt.executeUpdate();
			conn.commit();
		} catch (Exception exc) {
			System.out.println("Error when deleting albumID \"" + albumID + "\": " + exc.getMessage());
			items = -1;
		}
		return items;
	}


		/**
	 * Deletes an audiofile
	 * 
	 * @param trackID the ID of the track to delete
	 * @return Number of tracks deleted, or -1 if error occurs.
	 */
	public int deleteTrack(int trackID) {
		int count;
		try(PreparedStatement preparedS = conn.prepareStatement(
				"DELETE" +
				" FROM audiofile" +
				" WHERE TrackID = ?;"))
		{
			preparedS.setInt(1, trackID);
			count = preparedS.executeUpdate();
			conn.commit();
		} catch(SQLException e) {
			System.out.println("Error deleting trackID " + trackID + ": " + e.getMessage());
			count = -1;
		}
		return count;
	}
	
	/**
	 * Deletes a genre
	 * 
	 * @param genre Genre to be deleted.
	 * @return Number of genres deleted (0/1), or -1 if error occurs.
	 */
	public int deleteGenre(String genre) {
		int count;
		try(PreparedStatement pstmt = conn.prepareStatement(
			"DELETE" +
			" FROM genre" +
			" WHERE GenreID = ?;"))
		{
			pstmt.setString(1, genre);
			count = pstmt.executeUpdate();
			conn.commit();
		} catch(SQLException e) {
			System.out.println("Error deleting genre" + genre + ": " + e.getMessage());
			count = -1;
		}
		return count;
	}
	
	/**
	 * Deletes a label
	 * 
	 * @param label Name of label to delete.
	 * @return Number of labels deleted (0/1), or -1 if error occurs.
	 */
	public int deleteLabel(String label) {
		int labelID = getRecordLabelID(label);
		int count = 0;
		if(labelID == 0 || labelID == -1) {
			System.out.println("Label " + label + " not found");
		} else {
			try(PreparedStatement pstmt = conn.prepareStatement(
					"DELETE" +
					" FROM recordlabel" +
					" WHERE LabelID = ?;"))
			{
				pstmt.setInt(1, labelID);
				count = pstmt.executeUpdate();
				conn.commit();
			} catch(SQLException e) {
				System.out.println("Error deleting label" + label + ": " + e.getMessage());
				count = -1;
			}
		}
		return count;
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
