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
        Set<String> updateOptions = new LinkedHashSet<>();

        //add all the various menu options.
        //remember to add a case to processOption() for the preceding char!
        menuOptions.add("s: search the database");
        menuOptions.add("i: insert items into the database");
        menuOptions.add("u: update items in the database");
        menuOptions.add("d: delete items from the database");

        insertOptions.add("c: add new creator");
        insertOptions.add("t: add new audio file");
        insertOptions.add("a: add new album");
        insertOptions.add("g: add new genre");
        insertOptions.add("l: add new record label");
        insertOptions.add("n: add new country");
        insertOptions.add("b: back to main menu");

        searchOptions.add("c: search by creator");
        searchOptions.add("t: search by title");
        searchOptions.add("a: search by album");
        searchOptions.add("g: search by genre");
        searchOptions.add("l: search by record label");
        searchOptions.add("n: search by country");
        searchOptions.add("y: list all albums by media type");
        searchOptions.add("e: list tracks by explicit rating");
        searchOptions.add("b: back to main menu");
        
        updateOptions.add("a: update album");
        updateOptions.add("l: update record label");
        updateOptions.add("g: update genre description");
        updateOptions.add("c: update audiofile country");
        updateOptions.add("b: back to main menu");
        
        deleteOptions.add("c: delete a creator");
        deleteOptions.add("t: delete a title");
        deleteOptions.add("a: delete an album");
        deleteOptions.add("g: delete a genre");
        deleteOptions.add("l: delete a label");
        deleteOptions.add("b: back to main menu");
        
       


        //use try-with-resources block to ensure close regardless of success
        System.out.print("connecting to db...");
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("connected!");
            conn.setAutoCommit(false); //do we want autocommit on/off?
            query = new Queries(conn, in);

            //menu loop
            char lastOption = '\0';
            while (lastOption != 'q') {
                printMenu(menuOptions);
                lastOption = getNullableChar();
                switch (lastOption) {
                    case 's':
                        printMenu(searchOptions);
                        lastOption = getNullableChar();
                        if (lastOption != 'q' && lastOption != 'b') processSearch(lastOption);
                        break;
                    case 'i':
                        printMenu(insertOptions);
                        lastOption = getNullableChar();
                        if (lastOption != 'q' && lastOption != 'b') processInsert(lastOption);
                        break;
                    case 'd':
                        printMenu(deleteOptions);
                        lastOption = getNullableChar();
                        if (lastOption != 'q' && lastOption != 'b') processDelete(lastOption);
                        break;
                    case 'u':
                        printMenu(updateOptions);
                        lastOption = getNullableChar();
                        if (lastOption != 'q' && lastOption != 'b') processUpdate(lastOption);
                        break;

                    default:
                        System.out.println("Error: unrecognized option (" + lastOption + "). Try again.");
                        break;
                    case 'q':
                    case 'b':
                        break;
                }
            }
        } catch (SQLException e) {
            System.out.println("\nError: could not connect to database. Wrong url or login info?");
        }
    }

    //switch for all the various search options that may be called
    private static void processSearch(char lastOption) {
        String albumName, creatorName, titleName;
        switch (lastOption) {
            //artist search
            case 'c':
                System.out.print("Enter a creator name to search for: ");
                query.queryByCreator(in.nextLine(), false);
                break;

            //title search
            case 't':
                System.out.print("Enter a title to search for: ");
                query.queryByAudioTitle(in.nextLine(), false);
                break;

            //album search
            case 'a':
                System.out.print("Enter an album name to search for: ");
                query.queryByAlbumTitle(in.nextLine(), true);
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

            case 'n':
                System.out.print("Enter country name: ");
                query.getTracksByCountry(in.nextLine());
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
                    System.out.print("Enter creator for new track: ");
                    String trackCreator = in.nextLine();
                    System.out.print("Enter track duration in seconds: ");
                    Integer duration = requestInt();
                    System.out.print("Enter y if track is explicit, anything else for clean: ");
                    Integer rating = (getNullableChar() == 'y') ? 1 : 0;
                    System.out.print("Enter country ID or leave blank for null ('?' for list of codes): ");
                    Integer countryID = getNullableInteger("country");

                    //attempt adding track to db
                    int trackID = query.insertAudiofile(track, rating, duration, countryID, albumID, trackCreator);
                    if (trackID > 0) successCount++;
                    System.out.print("Enter a genre for the new track of leave blank for null: ");
                    String genre = getNullableString();
                    if (genre != null) {
                        query.addGenreToTrack(trackID, genre);
                    }
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
                System.out.print("Enter creator for new track: ");
                String creator = in.nextLine();
                System.out.print("Enter name for new track: ");
                String track = in.nextLine();
                System.out.print("Enter track duration in seconds: ");
                Integer duration = requestInt();
                System.out.print("Enter y if track is explicit, anything else for clean: ");
                Integer rating = (getNullableChar() == 'y') ? 1 : 0;
                System.out.print("Enter country ID or leave blank for null ('?' for list of codes): ");
                Integer countryID = getNullableInteger("country");
                int trackID = query.insertAudiofile(track, rating, duration, countryID, aID, creator);
                System.out.print("Enter a genre for the new track of leave blank for no genre: ");
                String genre = getNullableString();
                if (genre != null) {
                    query.addGenreToTrack(trackID, genre);
                }
                //todo: implement t
                break;

              //add new record label
            case 'l':
            	System.out.print("Enter name of new Record Label: ");
            	String labelName = in.nextLine();
            	System.out.print("Enter founding date as yyyymmdd (eg. 20190523) or leave blank for null: ");
                String foundDate = getNullableString();
                System.out.print("Enter country ID or leave blank for null ('?' for list of codes): ");
                Integer rlCountryID = getNullableInteger("country");
            	query.insertRecordLabel(labelName, foundDate, rlCountryID);
                break;
                
            //add new creator
            case 'c':
            	System.out.print("Enter name of new Creator: ");
            	String creatorName = in.nextLine();
            	query.insertCreator(creatorName);
                break;
             
            //add new genre
            case 'g':
            	System.out.print("Enter name of new genre: ");
            	String recLab = in.nextLine();
            	System.out.print("Enter description of genre or leave blank for null: ");
            	String descrip = in.nextLine();
            	query.insertGenre(recLab, descrip);
            	break;

            //add new country
            case 'n':
            	System.out.print("Enter name of country: ");
            	String countryName = in.nextLine();
            	query.insertCountry(countryName);
            	break;
            	
            default:
                System.out.print("Unrecognized menu option (" + lastOption + "). Please try again.");
                break;
        }
    }
    
  //switch for all the various search options that may be called
    private static void processUpdate(char lastOption) {
        switch (lastOption) {
        
	        //label update
	        case 'l':
	            System.out.print("Enter the record label name to update: ");
	            String lName = in.nextLine();
	            System.out.println("d: founding date"
	            		+ "\nc: country"
	            		+ "\nWhich would you like to update?");
	            String update = in.nextLine();
	            if(update.equals("d")) {
	            	System.out.print("Enter the Founding Date of the label in yyyymmdd format or leave blank for null: ");
	            	String lDate = getNullableString();
	            	query.updateLabelDate(lName, lDate);
	            }
	            else if(update.equals("c")) {
	            	System.out.print("Enter country ID or leave blank for null ('?' for list of codes): ");
	            	Integer lCountry = getNullableInteger("country");
	            	query.updateLabelCountry(lName, lCountry);
	            }
	            break;
	          
	        //album update
	        case 'a':
	            System.out.print("Enter the album name to update: ");
	            String aName = in.nextLine();
	            System.out.println("d: release date"
	            		+ "\nl: record label"
	            		+ "\nWhich would you like to update?");
	            String aUpdate = in.nextLine();
	            if(aUpdate.equals("d")) {
	            	System.out.print("Enter the release date of the album in yyyymmdd format or leave blank for null: ");
	            	String rDate = getNullableString();
	            	query.updateAlbumRD(aName, rDate);
	            }
	            else if(aUpdate.equals("l")) {
	            	System.out.print("Enter record label for album or leave blank for null: ");
	            	String aLabel = getNullableString();
	            	query.updateAlbumRL(aName, aLabel);
	            }
	            break;
            
            //genre update
            case 'g':
                System.out.print("Enter the genre name to update: ");
                String name = in.nextLine();
                System.out.print("Enter description of genre or leave blank for null: ");
            	String descrip = in.nextLine();
                query.updateGenre(name, descrip);
                break;

            //audio file country update
            case 'c':
                System.out.print("Enter track name: ");
                String t_name = in.nextLine();
                System.out.print("Enter new country name to associate with track: ");
                String c_name = in.nextLine();
                query.updateCountryIDaf(t_name, c_name);
                break;

            default:
                System.out.println("Unrecognized menu option (" + lastOption + "). Please try again.");
                break;
        }
    }

    //switch for all the various delete options that may be called
    //prompt for any extra information as needed, then call some jdbc handler method
    private static void processDelete(char lastOption) {
    	String creator, audiofile, album, genre, label;
    	int x, count;
        switch (lastOption) {
        	case 'c':
        		System.out.print("Please enter the name of the creator you would like to delete: ");
        		creator = in.nextLine();
        		count = query.queryByCreator(creator, true);
                if (count > 0) {
                    System.out.print("Please enter an ID from the list above to delete: ");
                    int creatorID = requestInt();
                    x = query.deleteCreator(creatorID);
                    if (x > 0) {
                        System.out.println("Successfully deleted " + x + " item" + ((x>1) ? "s" : ""));
                    } else {
                        System.out.println("Unable to delete requested item");
                    }
                }
        		break;
        	
        	case 't':
        		System.out.print("Please enter the name of the track you would like to delete: ");
        		audiofile = in.nextLine();
        		count = query.queryByAudioTitle(audiofile, true);
                if (count > 0) {
                    System.out.print("Please enter an ID from the list above to delete: ");
                    int trackID = requestInt();
                    x = query.deleteTrack(trackID);
                    if (x > 0) {
                        System.out.println("Successfully deleted " + x + " item" + ((x>1) ? "s" : ""));
                    } else {
                        System.out.println("Unable to delete requested item");
                    }
                }
        		break;
        	
        	case 'a':
        		System.out.print("Please enter the name of the album you would like to delete: ");
        		album = in.nextLine();
        		count = query.queryByAlbumTitle(album, false);
                if (count > 0) {
                    System.out.println("Please enter an ID from the list above to delete: ");
                    int albumID = requestInt();
                    x = query.deleteAlbum(albumID);
                    if (x > 0) {
                        System.out.println("Successfully deleted " + x + " item" + ((x>1) ? "s" : ""));
                    } else {
                        System.out.println("Unable to delete requested item");
                    }
                }
        		break;
        	
        	case 'g':
        		System.out.print("Please enter the name of the genre you would like to delete: ");
        		genre = in.nextLine();
        		x = query.deleteGenre(genre);
                if (x > 0) {
                    System.out.println("Successfully deleted " + x + " item" + ((x>1) ? "s" : ""));
                } else {
                    System.out.println("Unable to delete requested item");
                }
        		break;
        	
        	case 'l':
        		System.out.print("Please enter the name of the label you would like to delete: ");
                label = in.nextLine();
                x = query.deleteLabel(label);
                if (x > 0) {
                    System.out.println("Successfully deleted " + x + " item" + ((x>1) ? "s" : ""));
                } else {
                    System.out.println("Unable to delete requested item");
                }
        		break;
        	
        	case 'b':
        		break;
        		
        	default:
        		System.out.println("Unrecognized menu option (" + lastOption + "). Please try again.");
        		break;
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
            System.out.print("Please enter the database URL (e.g. jdbc:mysql://localhost:3306/world): ");
            url = in.nextLine();
            System.out.print("Please enter your database username: ");
            username = in.nextLine();
            System.out.print("Please enter your database password: ");
            password = in.nextLine();
            System.out.print("Please enter the driver you wish to use (e.g. com.mysql.cj.jdbc.Driver): ");
            driver = in.nextLine();
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
    //passes String context to printCodes() function if first char is '?', but still requires int to break loop
    private static Integer getNullableInteger(String context) {
        int val = -1;
        do {
            String s = in.nextLine();
            //check for code list request in the form of ? as first char
            if (s.length() > 0 && s.charAt(0) == '?') {
                printCodes(context); //pass request for code list to print code switch
                System.out.print("Enter code: "); //reprint request for code before loop resumes
            }
            else {
                try {
                    val = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    if (e.getMessage().contains("input string: \"\"")) return null;
                    System.out.print("That was not a properly formatted single number. Please try again: ");
                }
            }
        } while (val < 0);
        return val;
    }

    //allows printing of IDs as needed.
    //just add a new case to the switch and call whatever print you need.
    private static void printCodes(String context) {
        switch (context.toLowerCase()) {
            case "country":
                query.printCountryCodes();
        }
    }
}
