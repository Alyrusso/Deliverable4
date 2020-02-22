package main.java;

import java.sql.*;
import java.util.Objects;
import java.util.Random;

public class QueryHandler {
    private Connection conn;
    private Statement statement;
    private ResultSet rs;
    private PreparedStatement pStatement;

    private Random rand;

    public QueryHandler(Connection c) {
        this.conn = c;
        rand = new Random(); //seed as 1000 for repeatable testing purposes
    }

    //returns a random int for use as IDs
    private int getID(String s) {
        return rand.nextInt(Integer.MAX_VALUE);
    }


    /** Inserts an album into the database
     * @param albumName Name of the album to insert
     * @param date  Date the album came out. Format as a number like "20171231"
     * @param label Name of the record label.
     * @return the albumID of the inserted album. -1 if insert failed.
     */
    public int insertAlbum(String albumName, String date, String label) {
        int albumID = -1;
        int labelID = 0;
        if (label != null) labelID = insertRecordLabel(label, null, 0);
        try {
            pStatement = conn.prepareStatement(
                    "INSERT INTO adb.album (AlbumID, AlbumName, MediaType, ReleaseDate, LabelID)\n" +
                    "VALUES (?, ?, 'Music', ?, ?);");
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

    /**
     * @param label Label name to search for
     * @return the first labelID matching the label name. 0 if not present, or -1 if error encountered.
     */
    public int getRecordLabelID(String label) {
        try {
            pStatement = conn.prepareStatement(
                    "SELECT LabelID " +
                    " FROM recordlabel" +
                    " WHERE recordlabel.Name" +
                    " LIKE ?;");
            pStatement.setString(1, label);
            rs = pStatement.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString("LabelID"));
                return Integer.parseInt(rs.getString("LabelID"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }




    //disconnects all DB resources if initialized
    public void disconnect() {
        System.out.print("closing db connection...");
        try {
            //close all DB resources
            if (rs != null) rs.close();
            if (pStatement != null) pStatement.close();
            if (statement != null) statement.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("closed!");
    }
}
