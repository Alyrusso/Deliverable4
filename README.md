# Zene

## Compiling the program
Navigate your terminal of choice to the folder named src and run the following command:  
    `javac -encoding utf8 ./main/java/Zene.java `   
    
Note: utf-8 encoding is required to ensure proper display output.    
Note: this program requires Java version 9 or greater to compile and run.

## Running the program
There are two main ways to run Zene:

###### Basic:
`java -cp "/path/to/driver/;" main.java.Zene`  
The program will prompt you for connection information such as url and login details.

###### With Arguments:
`java -cp "/path/to/driver/;" main.java.Zene <db_url> <username> <password> <driver_class>`

###### Full Example:
`java -cp "C:\JDBCDrivers\;" main.java.Zene jdbc:mysql://localhost:3306/adb username password com.mysql.cj.jdbc.Driver`


## Usage
Follow the menu prompts.