package main.java;

import jdk.swing.interop.SwingInterOpUtils;

import java.sql.*;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Entry point and menu handler for group 17 music database application
 */
public class Zene {
    private static QueryHandler query;

    private static Set<String> menuOptions;
    private static Scanner in;

    private static String url;
    private static String username;
    private static String password;

    public static void main(String[] args) {
        //initialize member objects
        in = new Scanner(System.in);
//        in.useDelimiter("\\n");
        verifyArgs(args);
        menuOptions = new LinkedHashSet<>();

        //add all the various menu options.
        //remember to add a case to processOption() for the preceding char!
        menuOptions.add("a: artist search");
        menuOptions.add("t: title search");
        menuOptions.add("n: add new audio file");
        menuOptions.add("u: add new album");

        query = new QueryHandler(connectDB());

        //menu loop
        char lastOption = '\0';
        while (lastOption != 'q') {
            printOptions();
            lastOption = Character.toLowerCase(in.next().charAt(0));
            if (lastOption != 'q') processOption(lastOption);
        }

        query.disconnect();
    }

    //switch for all the various options that may be called
    //prompt for any extra information as needed, then call some jdbc handler method
    private static void processOption(char lastOption) {
        switch (lastOption) {
            //artist search
            case 'a':
                System.out.print("Enter an artist name to search for: " );
                String artistName = in.next();
                //call some kind of JDBC select query method here I guess, like:
                //query.queryByCreator(artistName);
                break;

            //title search
            case 't':
                System.out.print("Enter a title to search for: " );
                String titleName = in.next();
                //call some kind of JDBC select query method here I guess, like:
                //query.queryByTitle(titleName);
                break;

            //add new album
            case 'u':
                System.out.print("Enter the name of the album to insert: ");
                in.nextLine();
                String albumName = in.nextLine();
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
    private static void printOptions() {
        for (String s: menuOptions)
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
