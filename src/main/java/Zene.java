package main.java;

import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Entry point and menu handler for group 17 music database application
 */
public class Zene {
    private static Set<String> menuOptions;
    private static Scanner in;

    public static void main(String[] args) {
        //initialize member objects
        in = new Scanner(System.in);
        menuOptions = new LinkedHashSet<>();

        //add all the various menu options.
        //use "\033[4m" to start underline. use "\033[0m" to return to normal
        //remember to add a case to processOption() for the underlined char!
        menuOptions.add("\033[4ma\033[0mrtist search");
        menuOptions.add("\033[4mt\033[0mitle search");
        menuOptions.add("add \033[4mn\033[0mew audio file");
        menuOptions.add("add new alb\033[4mu\033[0mm");

        //menu loop
        char lastOption = '\0';
        while (lastOption != 'q') {
            printOptions();
            lastOption = Character.toLowerCase(in.next().charAt(0));
            if (lastOption != 'q') processOption(lastOption);
        }
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
        int count = 0;
        for (String s: menuOptions) {
            System.out.printf("\t%d: %s\n",++count, s);
        }
        System.out.print("Select menu option by underlined character (or q to exit): ");
    }
}
