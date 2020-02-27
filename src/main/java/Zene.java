package main.java;

import java.sql.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Entry point and menu handler for group 17 music database application
 */
public class Zene {
    private static Queries query;

    private static Scanner in;

    private static String url;
    private static String username;
    private static String password;

    public static void main(String[] args) {
        //initialize member objects
        in = new Scanner(System.in);
        in.useDelimiter("\\n");
        verifyArgs(args);
        Set<String> menuOptions = new LinkedHashSet<>();
        Set<String> insertOptions = new LinkedHashSet<>();
        Set<String> deleteOptions = new LinkedHashSet<>();
        Set<String> searchOptions = new LinkedHashSet<>();

        //add all the various menu options.
        //remember to add a case to processOption() for the preceding char!
        menuOptions.add("s: search the database");
        menuOptions.add("i: insert items into the database");
        menuOptions.add("d: delete items from the database");

        insertOptions.add("c: add new creator");
        insertOptions.add("t: add new audio file");
        insertOptions.add("a: add new album");
        insertOptions.add("b: back to main menu");

        searchOptions.add("c: search by creator");
        searchOptions.add("t: search by title");
        searchOptions.add("a: search by album");
        searchOptions.add("g: search by genre");
        searchOptions.add("l: search by label");
        searchOptions.add("y: list all albums by media type");
        searchOptions.add("e: list all explicit tracks");
        searchOptions.add("b: back to main menu");

        query = new Queries(connectDB());

        //menu loop
        char lastOption = '\0';
        while (lastOption != 'q') {
            printMenu(menuOptions);
            lastOption = Character.toLowerCase(in.nextLine().charAt(0));
            switch (lastOption) {
                case 's':
                    printMenu(searchOptions);
                    lastOption = Character.toLowerCase(in.nextLine().charAt(0));
                    if (lastOption != 'q' && lastOption != 'b') processSearch(lastOption);
                    break;
                case 'i':
                    printMenu(insertOptions);
                    lastOption = Character.toLowerCase(in.nextLine().charAt(0));
                    if (lastOption != 'q' && lastOption != 'b') processInsert(lastOption);
                    break;
                case 'd':
                    printMenu(deleteOptions);
                    lastOption = Character.toLowerCase(in.nextLine().charAt(0));
                    if (lastOption != 'q' && lastOption != 'b') processDelete(lastOption);
                    break;

                default:
                    System.out.println("Error: unrecognized option (" + lastOption + "). Try again.");
                    break;
                case 'q':
                case 'b':
                    break;
            }
        }

        query.disconnect();
    }


    //switch for all the various search options that may be called
    private static void processSearch(char lastOption) {
        String albumName, creatorName, titleName;
        switch (lastOption) {
            //artist search
            case 'c':
                System.out.print("Enter a creator name to search for: ");
                query.queryByCreator(in.nextLine());
                break;

            //title search
            case 't':
                System.out.print("Enter a title to search for: ");
                query.queryByAudioTitle(in.nextLine());
                break;

            //album search
            case 'a':
                System.out.print("Enter an album name to search for: ");
                query.queryByAlbumTitle(in.nextLine());
                break;

                //genre search
            case 'g':
                System.out.print("Enter a genre name to search for: ");
                query.queryByGenre(in.nextLine());
                break;

            case 'l':
                System.out.print("Enter a label name to search for: ");
                query.getTracksLabel(in.nextLine());
                break;

            case 'y':
                System.out.print("Enter a media type to list: ");
                query.queryByMediaType(in.nextLine());
                break;

            case 'e':
                System.out.print("Enter 'mature' for explicit tracks only or 'everyone' for censored track list: ");
                String dec = in.nextLine();
                if(dec.compareTo("mature") == 0 || dec.compareTo("Mature") == 0 ){
                    query.getTracksByRating(1);
                }else{
                    query.getTracksByRating(0);
                }
                break;
        }
    }

    //switch for all the various insert options that may be called
    //prompt for any extra information as needed, then call some jdbc handler method
    private static void processInsert(char lastOption) {
        switch (lastOption) {
            //add new album
            case 'a':
                System.out.print("What type of media is this album (e.g. Music, Podcast): ");
                String mediaType = in.nextLine();
                System.out.print("Enter the name of the album to insert: ");
                String albumName = in.nextLine();
                System.out.print("Enter release date as yyyymmdd (eg. 20190523) or leave blank for null: ");
                String date = getNullableString();
                System.out.print("Enter a record label name or leave blank for null: ");
                String label = getNullableString();
                int albumID = query.insertAlbum(albumName, date, label, mediaType);
                System.out.print("How many tracks would you like to add: ");
                int trackCount = requestInt();
                int successCount = 0;
                for (int i=0; i < trackCount; ++i) {
                    System.out.print("Enter name for new track: ");
                    String track = in.nextLine();
                    System.out.print("Enter track duration in seconds: ");
                    Integer duration = requestInt();
                    System.out.print("Enter y if track is explicit, anything else for clean: ");
                    Integer rating = (getNullableChar() == 'y') ? 1 : 0;
                    System.out.print("Enter country ID or leave blank for null: ");
                    Integer countryID = null;
                    if (getNullableChar() != ' ') countryID = requestInt();

                    //attempt adding track to db
                    int trackID = query.insertAudiofile(track, rating, duration, countryID, albumID);
                    if (trackID > 0) successCount++;
                }
                if (successCount > 0) System.out.println("Successfully added " + successCount + " tracks to album " + albumName);
                if (successCount != trackCount) System.out.println(trackCount - successCount + " tracks could not be added.");
                break;

            //add new audio file
            case 't':
                System.out.println("Note, inserting a track requires knowing the albumID. Adding by album includes the option to insert tracks afterwards.");
                System.out.print("Enter y to continue, or anything else to return to main menu: ");
                char cont = getNullableChar();
                if (cont != 'y') break;
                System.out.print("Enter album ID for new track: ");
                Integer aID = requestInt();
                System.out.print("Enter name for new track: ");
                String track = in.nextLine();
                System.out.print("Enter track duration in seconds: ");
                Integer duration = requestInt();
                System.out.print("Enter y if track is explicit, anything else for clean: ");
                Integer rating = (getNullableChar() == 'y') ? 1 : 0;
                System.out.print("Enter country ID or leave blank for null: ");
                Integer countryID = getNullableInteger();
                query.insertAudiofile(track, rating, duration, countryID, aID);

                //todo: implement t
                break;

            //add new creator
            case 'c':
                //todo implement insert creator
                break;

            default:
                System.out.println("Unrecognized menu option (" + lastOption + "). Please try again.");
                break;
        }
    }

    //switch for all the various insert options that may be called
    //prompt for any extra information as needed, then call some jdbc handler method
    private static void processDelete(char lastOption) {
        switch (lastOption) {

        }
    }

    //helper method for printing all menu options
    private static void printMenu(Set<String> menu) {
        for (String s: menu)
            System.out.println("\t" + s);
        System.out.print("Select menu option by preceding character (or q to exit): ");
    }

    //called from main, verifies args are proper
    private static void verifyArgs(String[] args) {
        //check for sufficient args, prompt for user input otherwise
        String driver;
        if (args.length < 4) {
            System.out.println("Please enter the database URL (e.g. jdbc:mysql://localhost:3306/world)");
            url = in.next();
            System.out.println("Please enter your database username: ");
            username = in.next();
            System.out.println("Please enter your database password: ");
            password = in.next();
            System.out.println("Please enter the driver you wish to use (e.g. com.mysql.cj.jdbc.Driver)");
            driver = in.next();
        } else {
            url = args[0];
            username = args[1];
            password = args[2];
            driver = args[3];
        }

        //check for proper URI protocol
        if (!url.toLowerCase().contains("jdbc:"))
            throw new IllegalArgumentException("<url> must start with \"jdbc:\"");

        //attempt to load driver class for jdbc
        try {Class.forName(driver);}
        catch (ClassNotFoundException e){
            System.out.println("Error: driver not found");
            System.exit(1);
        }
    }

    //establishes connection to database and initializes query handler
    private static Connection connectDB() {
        System.out.print("connecting to db...");
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, username, password);
            conn.setAutoCommit(false); //do we want autocommit on/off?
        } catch (SQLException e) {
            System.out.println("\nError: could not connect to database. Wrong url or login info?");
            System.exit(1);
        }
        System.out.println("connected!");
        return conn;
    }

    //formats user input as number, continually prompts if not proper int
    private static int requestInt() {
        int val = -1;
        do {
            String s = in.nextLine();
            try {
                val = Integer.parseInt(s);
            } catch (NumberFormatException e) {System.out.print("That was not a properly formatted single number. Please try again: ");}
        }
        while (val < 0);
        return val;
    }

    //private helper method to get a single char input, when empty input is allowed
    //returns the first char of whatever a user inputs, or space if input is blank
    //converts to lowercase
    private static char getNullableChar() {
        String s = in.nextLine();
        try {return s.toLowerCase().charAt(0);}
        catch (StringIndexOutOfBoundsException e) {return ' ';}
    }

    //returns empty user input as null rather than zero-length String
    private static String getNullableString() {
        String s = in.nextLine();
        if (s.length() == 0) return null;
        return s;
    }

    //tries to return users input as Integer, or null if input was blank. loops if input cannot be made into integer
    private static Integer getNullableInteger() {
        int val = -1;
        do {
            String s = in.nextLine();
            try {
                val = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                if (e.getMessage().contains("input string: \"\"")) return null;
                System.out.print("That was not a properly formatted single number. Please try again: ");
            }
        } while (val < 0);
        return val;
    }
}
