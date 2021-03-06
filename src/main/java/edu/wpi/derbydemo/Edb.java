package edu.wpi.derbydemo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Scanner;

import static java.lang.System.exit;

public class Edb {
    public static void main(String[] args) throws SQLException, IOException {
        String arg, inputUsername = "", inputPassword = "", inputCommand = "";
        int i = 0;

        if (args.length < 2) {
            System.out.println("Please run again, and enter username and password.");
            exit(0);
        }
        //Parse username and password
        while (i < args.length) {
            arg = args[i++]; //Get next arg.
            //System.out.println("i = " + i + " arg = " + arg);
            if (i == 1)
                inputUsername = arg;
            if (i == 2)
                inputPassword = arg;
            if (i == 3)
                inputCommand = arg;
        }
        if (args.length == 2) {
            System.out.println("1 - Report Museum Information");
            System.out.println("2 - Report Paintings in Museum");
            System.out.println("3 - Update Museum Phone Number");
            System.out.println("4 - Exit Program");
            exit(0);
        }
        //System.out.println("Username = " + inputUsername + " Password = " + inputPassword);
        System.out.println("-------Embedded Apache Derby Connection Testing --------");
        try {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        } catch (ClassNotFoundException e) {
            System.out.println("Apache Derby Driver not found. Add the classpath to your module.");
            System.out.println("For IntelliJ do the following:");
            System.out.println("File | Project Structure, Modules, Dependency tab");
            System.out.println("Add by clicking on the green plus icon on the right of the window");
            System.out.println("Select JARs or directories. Go to the folder where the database JAR is located");
            System.out.println("Click OK, now you can compile your program and run it.");
            e.printStackTrace();
            return;
        }

        System.out.println("Apache Derby driver registered!");
        Connection connection = null;

        try {
            // substitute your database name for myDB
            connection = DriverManager.getConnection(String.format("jdbc:derby:memory:Edb;create=true;user=%s;password=%s", inputUsername, inputPassword));
            turnOnBuiltInUsers(connection, inputUsername, inputPassword);
            Statement stmt = connection.createStatement();

            //Create tables if they do not already exist
            String query = "CREATE TABLE Museums( Name VARCHAR(50) NOT NULL, Address VARCHAR(255), Phone VARCHAR(50), Visitors INT, PRIMARY KEY (Name))";
            stmt.execute(query);
            String query2 = "CREATE TABLE Paintings( Museum VARCHAR(50) NOT NULL, Name VARCHAR(255) NOT NULL, Type VARCHAR(50), Artist VARCHAR(50), PRIMARY KEY (Name))";
            stmt.execute(query2);
        } catch (SQLException e) {
            System.out.println("Connection failed. Check output console.");
            e.printStackTrace();
            return;
        }
        System.out.println("Apache Derby connection established!");
        setupDB(connection, "museum.csv");
        setupDB(connection, "paintings.csv");
        connection.commit();
        System.out.println(inputCommand);
        int option = Integer.parseInt(inputCommand);
        parseInput(connection, option);
        connection.commit();
        connection.close();
    }

    //Parses input. Input parameters are connection, and the option that the user input
    /*System.out.println("1 - Report Museum Information");
            System.out.println("2 - Report Paintings in Museum");
            System.out.println("3 - Update Museum Phone Number");
            System.out.println("4 - Exit Program");*/
    private static void parseInput(Connection connection, int option) throws SQLException, IOException {
        Statement stmt = connection.createStatement();
        Statement stmt2 = connection.createStatement();
        if (option == 1) {//Report Museum Information
            ResultSet rs = stmt.executeQuery("SELECT * FROM Museums");
            printMuseumInfo(rs);
        } else if (option == 2) {//Report Painting Information
            ResultSet rs2 = stmt.executeQuery("SELECT * FROM Museums");
            System.out.println("Name\t\t|\t\tAddress\t\t|\t\tPhone\t\t|\t\tVisitors/Year");
            while (rs2.next()) {
                String s1 = rs2.getString("Name");
                String s2 = rs2.getString("Address");
                String s3 = rs2.getString("Phone");
                int s4 = rs2.getInt("Visitors");
                System.out.println(s1 + "\t|\t" + s2 + "\t|\t" + s3 + "\t|\t" + s4);
                System.out.println("Museum\t\t|\t\tName\t\t|\t\tType\t\t|\t\tArtist");

                ResultSet rs1 = stmt2.executeQuery(String.format("SELECT * FROM PAINTINGS WHERE MUSEUM = '%s'", s1));
                while (rs1.next()) {
                    String s5 = rs1.getString("Museum");
                    String s6 = rs1.getString("Name");
                    String s7 = rs1.getString("Type");
                    String s8 = rs1.getString("Artist");
                    System.out.println(s5 + "\t|\t" + s6 + "\t|\t" + s7 + "\t|\t" + s8);
                }
            }
        } else if (option == 3) {
            Scanner in = new Scanner(System.in);
            System.out.println("Enter museum name you want to update (ex: Worcester Art Museum):");
            String inputMuseum = in.nextLine();
            System.out.println("New Phone Number:");
            String inputNumber = in.nextLine();
            in.close();
            System.out.println(inputMuseum);
            stmt.executeUpdate(String.format("UPDATE Museums SET Phone = '%s' WHERE Name = '%s'", inputNumber, inputMuseum));
            System.out.println("Successfully changed phone number.");
            ResultSet rs = stmt2.executeQuery("SELECT * FROM Museums");
            printMuseumInfo(rs);
        } else {
            System.out.println("Exiting.");
            exit(0);
        }
    }

    //Duplicated code from parseInput that prints the museums table
    private static void printMuseumInfo(ResultSet rs) throws SQLException {
        System.out.println("Name\t\t|\t\tAddress\t\t|\t\tPhone\t\t|\t\tVisitors/Year");
        while (rs.next()) {
            String s1 = rs.getString("Name");
            String s2 = rs.getString("Address");
            String s3 = rs.getString("Phone");
            int s4 = rs.getInt("Visitors");
            System.out.println(s1 + "\t|\t" + s2 + "\t|\t" + s3 + "\t|\t" + s4);
        }
    }

    //Reads from CSV file and populates DB.
    private static void setupDB(Connection connection, String filename) throws SQLException, IOException {
        Statement stmt = connection.createStatement();
        String s1 = null, s2 = null, s3 = null, s4 = null, dbName = null;
        if (filename.equals("museum.csv")) {
            s1 = "Name";
            s2 = "Address";
            s3 = "Phone";
            s4 = "Visitors";
            dbName = "Museums";
        } else {
            s1 = "Museum";
            s2 = "Name";
            s3 = "Type";
            s4 = "Artist";
            dbName = "Paintings";
        }
        String sql = String.format("INSERT INTO %s(%s, %s, %s, %s) VALUES (?, ?, ?, ?)", dbName, s1, s2, s3, s4);
        //System.out.println("SQL = " + sql);
        PreparedStatement statement = connection.prepareStatement(sql);

        //Get absolute file path
        String path = "/" + filename;

        //System.out.println(path);
        BufferedReader lineReader = new BufferedReader(new InputStreamReader(Edb.class.getResourceAsStream(path)));
        String lineText = null;

        int count = 0;
        int batchSize = 20;

        lineReader.readLine(); // skip header line
        while ((lineText = lineReader.readLine()) != null) {
            //System.out.println(lineText);
            String[] data = lineText.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

            String name = data[0];
            String address = data[1];
            String phone = data[2];
            String visitors = data[3];

            //Check if for museum or painting table
            if (filename.equals("museum.csv")) {
                statement.setString(1, name);
                statement.setString(2, address);
                statement.setString(3, phone);
                int visitorsCount = Integer.parseInt(visitors);
                statement.setInt(4, visitorsCount);
            } else {
                statement.setString(1, name);
                statement.setString(2, address);
                statement.setString(3, phone);
                statement.setString(4, visitors);
            }

            statement.addBatch();
            count++;
            if (count % batchSize == 0) {
                statement.executeBatch();
            }
        }

        lineReader.close();

        // execute the remaining queries
        statement.executeBatch();
    }

    //Turns on built in users and creates the necessary admin user.
    private static void turnOnBuiltInUsers(Connection connection, String username, String pass) throws SQLException {
        System.out.println("Turning on authentication.");
        Statement s = connection.createStatement();
        // Setting and Confirming requireAuthentication
        s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.connection.requireAuthentication', 'true')");
        ResultSet rs = s.executeQuery("VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('derby.connection.requireAuthentication')");
        rs.next();
        System.out.println("Value of requireAuthentication is " + rs.getString(1));
        // Setting authentication scheme to Derby
        s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.authentication.provider', 'BUILTIN')");

        // Creating users
        //s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" + "'derby.user." + username + "', '" + pass + "')");
        String un = "'" + username + "'";
        String pw = "'" + pass + "'";
        s.executeUpdate(String.format("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user.%s',%s)", username, pw));
        s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" + "'derby.user.readonly', 'password')");
        System.out.println("Successfully created user");

        // Setting default connection mode to no access
        // (user authorization)
        s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.defaultConnectionMode', 'noAccess')");
        // Confirming default connection mode
        rs = s.executeQuery("VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('derby.database.defaultConnectionMode')");
        rs.next();
        System.out.println("Value of defaultConnectionMode is " + rs.getString(1));

        // Defining full access usersusers
        s.executeUpdate(String.format("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.fullAccessUsers',%s)", un));

        // Confirming full-access users
        rs = s.executeQuery("VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('derby.database.fullAccessUsers')");
        rs.next();
        System.out.println("Value of fullAccessUsers is " + rs.getString(1));

        // Confirming read-only users
        rs = s.executeQuery("VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('derby.database.readOnlyAccessUsers')");
        rs.next();
        System.out.println("Value of readOnlyAccessUsers is " + rs.getString(1));

        // We would set the following property to TRUE only
        // when we were ready to deploy.
        s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" + "'derby.database.propertiesOnly', 'false')");
        s.close();
    }
}

