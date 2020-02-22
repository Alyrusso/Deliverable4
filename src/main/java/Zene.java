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
//        in.useDelimiter("\\n");
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

        insertOptions.add("n: add new audio file");
        insertOptions.add("u: add new album");
        insertOptions.add("b: back to main menu");

        searchOptions.add("c: creator search");
        searchOptions.add("t: title search");
        searchOptions.add("l: album search");
        searchOptions.add("b: back to main menu");

        query = new Queries(connectDB());

        //menu loop
        char lastOption = '\0';
        while (lastOption != 'q') {
            printMenu(menuOptions);
            lastOption = Character.toLowerCase(in.next().charAt(0));
            switch (lastOption) {
                case 's':
                    printMenu(searchOptions);
                    lastOption = Character.toLowerCase(in.next().charAt(0));
                    break;
                case 'i':
                    printMenu(insertOptions);
                    lastOption = Character.toLowerCase(in.next().charAt(0));
                    break;
                case 'd':
                    printMenu(deleteOptions);
                    lastOption = Character.toLowerCase(in.next().charAt(0));
                    break;
                default:
                    System.out.println("Error: unrecognized option (" + lastOption + "). Try again.");
                    break;
            }
            if (lastOption != 'q' && lastOption != 'b') processOption(lastOption);
        }

        query.disconnect();
    }

    //switch for all the various options that may be called
    //prompt for any extra information as needed, then call some jdbc handler method
    private static void processOption(char lastOption) {
        String albumName, creatorName, titleName;
        switch (lastOption) {
            //artist search
            case 'c':
                System.out.print("Enter a creator name to search for: " );
                creatorName = in.next();
                query.queryByCreator(creatorName);
                break;

            //title search
            case 't':
                System.out.print("Enter a title to search for: " );
                titleName = in.next();
                query.queryByAudioTitle(titleName);
                break;

            //album search
            case 'l':
                System.out.print("Enter an album name to serach for: ");
                albumName = in.next();
                query.queryByAlbumTitle(albumName);
                break;

            //add new album
            case 'u':
                System.out.print("Enter the name of the album to insert: ");
                in.nextLine();
                albumName = in.nextLine();
                String label = null, date = null;
                System.out.print("Enter y to add a release date, or anything else to leave null: ");
                char cont = in.nextLine().toLowerCase().charAt(0);
                if (cont == 'y') {
                    System.out.print("Enter release date as yyyymmdd (eg. 20190523): ");
                    date = in.nextLine();
                }
                System.out.print("Enter y to add a record label, or anything else to leave null: ");
                cont = in.nextLine().toLowerCase().charAt(0);
                if (cont == 'y') {
                    System.out.print("Enter the name of the record label: ");
                    label = in.nextLine();
                }
                query.insertAlbum(albumName, date, label);
                break;

            //add new audio file
            case 'n':
                //todo: implement n
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
        }
        System.out.println("connected!");
        return conn;
    }
}
