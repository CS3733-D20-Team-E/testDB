package edu.wpi.derbydemo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

import static java.lang.System.exit;

public class Edb {
    public static void main(String[] args) throws SQLException, IOException {
        String arg, inputUsername = "", inputPassword = "";
        int i = 0;

        if (args.length < 2) {
            System.out.println("Please run again, and enter username and password.");
            exit(0);
        }
        //Parse username and password
        while (i < args.length) {
            arg = args[i++]; //Get next arg.
            System.out.println("i = " + i + " arg = " + arg);
            if (i == 1)
                inputUsername = arg;
            if (i == 2)
                inputPassword = arg;
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
            connection = DriverManager.getConnection("jdbc:derby:Edb;create=true;user=" + inputUsername + ";password=" + inputPassword);
            turnOnBuiltInUsers(connection, inputUsername, inputPassword);
            Statement stmt = connection.createStatement();
          /*  CREATE TABLE `Museums` (
	`Name` VARCHAR(50) NOT NULL,
	`Address` VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin,
	`Country` VARCHAR CHARACTER SET ascii COLLATE ascii_bin,
	`Visitors` INT unsigned zerofill,
    PRIMARY KEY (`Name`)
);*/
            String query = "CREATE TABLE Museums( " + "Name VARCHAR(50) NOT NULL," +
                    "Address VARCHAR(255), " + "Country VARCHAR(50), " +
                    "Visitors INT unsigned zerofill, " + "PRIMARY KEY (Name))";
            stmt.execute(query);
            String query2 = "CREATE TABLE Museums( " + "Museum VARCHAR(50) NOT NULL," +
                    "Name VARCHAR(255), " + "Type VARCHAR(50) NOT NULL, " +
                    "Artist VARCHAR(50), " + "PRIMARY KEY (Name))";
            stmt.execute(query2);
        } catch (SQLException e) {
            System.out.println("Connection failed. Check output console.");
            e.printStackTrace();
            return;
        }
        System.out.println("Apache Derby connection established!");
        setupDB(connection, "museum.csv");
        setupDB(connection, "paintings.csv");
    }

    //TODO: Read from 2 csv files and populate DB.
    private static void setupDB(Connection connection, String filename) throws SQLException, IOException {
        Statement stmt = connection.createStatement();
        String s1 = null, s2 = null, s3 = null, s4 = null, dbName = null;
        if(filename.equals("museum.csv")) {
            s1 = "Name";
            s2 = "Address";
            s3 = "Country";
            s4 = "Visitors";
            dbName = "Museums";
        }
        else {
            s1 = "Museum";
            s2 = "Name";
            s2 = "Type";
            s2 = "Artist";
        }
        String sql = String.format("INSERT INTO %s(%s, %s, %s, %s) VALUES (?, ?, ?, ?)", dbName, s1, s2, s3, s4);
        System.out.println("SQL = " + sql);
        PreparedStatement statement = connection.prepareStatement(sql);
        BufferedReader lineReader = new BufferedReader(new FileReader(filename));
        String lineText = null;

        int count = 0;
        int batchSize = 20;

        lineReader.readLine(); // skip header line
        while ((lineText = lineReader.readLine()) != null) {
            String[] data = lineText.split(",");
            String name = data[0];
            String address = data[1];
            String country = data[2];
            String visitors = data[3];
            String comment = data.length == 5 ? data[4] : "";

            statement.setString(1, name);
            statement.setString(2, address);


            Float fRating = Float.parseFloat(visitors);
            statement.setFloat(4, fRating);


            statement.addBatch();

            if (count % batchSize == 0) {
                statement.executeBatch();
            }
        }

        lineReader.close();

        // execute the remaining queries
        statement.executeBatch();

        connection.commit();
        connection.close();
    }
    private static void turnOnBuiltInUsers(Connection connection, String username, String pass) throws SQLException{
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
        System.out.println("Successfully created user");

        // Setting default connection mode to no access
        // (user authorization)
        s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.defaultConnectionMode', 'noAccess')");
        // Confirming default connection mode
        rs = s.executeQuery ("VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('derby.database.defaultConnectionMode')");
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

