package main.java;

import java.sql.*;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Entry point and menu handler for group 17 music database application
 */
public class Zene {
    private static Connection conn;
    private static Statement statement;
    private static ResultSet rs;
    private static PreparedStatement pStatement;

    private static Set<String> menuOptions;
    private static Scanner in;

    private static String url;
    private static String username;
    private static String password;
    private static String driver;

    public static void main(String[] args) {
        //initialize member objects
        in = new Scanner(System.in);
        verifyArgs(args);
        menuOptions = new LinkedHashSet<>();

        //add all the various menu options.
        //use "\033[4m" to start underline. use "\033[0m" to return to normal
        //remember to add a case to processOption() for the underlined char!
        menuOptions.add("a: artist search");
        menuOptions.add("t: title search");
        menuOptions.add("n: add new audio file");
        menuOptions.add("u: add new album");

        connectDB();

        //menu loop
        char lastOption = '\0';
        while (lastOption != 'q') {
            printOptions();
            lastOption = Character.toLowerCase(in.next().charAt(0));
            if (lastOption != 'q') processOption(lastOption);
        }

        disconnectDB();
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
                //queryByArtist(artistName);
                break;

            //title search
            case 't':
                System.out.print("Enter a title to search for: " );
                String titleName = in.next();
                //call some kind of JDBC select query method here I guess, like:
                //queryByTitle(titleName);
                break;

            //add new album
            case 'u':
                //todo: implement u
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
    }

    //disconnects all DB resources if initialized then exits the program
    private static void disconnectDB() {
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
        System.exit(0);
    }

    private static void connectDB() {
        System.out.print("connecting to db...");
        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url, username, password);
            //conn.setAutoCommit(false); //do we want autocommit on/off?
        } catch (ClassNotFoundException e) {
            System.out.println("Error: driver not found");
            disconnectDB();
        } catch (SQLException e) {
            System.out.println("Error: could not connect to database. Wrong url or login info?");
            disconnectDB();
        }
        System.out.println("connected!");
    }
}
