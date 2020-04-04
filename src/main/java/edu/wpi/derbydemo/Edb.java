package edu.wpi.derbydemo;

import java.sql.*;

import static java.lang.System.exit;

public class Edb {
    public static void main(String[] args) throws SQLException {
        String arg, inputUsername = "", inputPassword = "";
        int i = 0;

        if(args.length < 2) {
            System.out.println("Please run again, and enter username and password.");
            exit(0);
        }
        //Parse username and password
        while (i < args.length) {
            arg = args[i++]; //Get next arg.
            System.out.println("i = " + i + " arg = " + arg);
            if(i == 1)
                inputUsername = arg;
            if(i == 2)
                inputPassword = arg;
        }
        if(args.length == 2) {
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
            connection = DriverManager.getConnection("jdbc:derby:Edb;create=true;user="+inputUsername+";password="+inputPassword);
            turnOnBuiltInUsers(connection, inputUsername, inputPassword);
        } catch (SQLException e) {
            System.out.println("Connection failed. Check output console.");
            e.printStackTrace();
            return;
        }
        System.out.println("Apache Derby connection established!");
        setupDB(connection);
    }

    //TODO: Read from 2 csv files and populate DB.
    private static void setupDB(Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();
    }

    private static void turnOnBuiltInUsers(Connection connection, String username, String pass) throws SQLException{
        System.out.println("Turning on authentication.");
        Statement s = connection.createStatement();
        // Setting and Confirming requireAuthentication
        s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" + "'derby.connection.requireAuthentication', 'true')");
        ResultSet rs = s.executeQuery("VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY(" + "'derby.connection.requireAuthentication')");
        rs.next();
        System.out.println("Value of requireAuthentication is " + rs.getString(1));
        // Setting authentication scheme to Derby
        s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" + "'derby.authentication.provider', 'BUILTIN')");

        // Creating users
        //s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" + "'derby.user." + username + "', '" + pass + "')");
        String un = "'" + username + "'";
        String pw = "'" + pass + "'";
        s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" + "'derby.user." + username + "'," + pw + ")");
        System.out.println("Successfully created user");

        // Setting default connection mode to no access
        // (user authorization)
        s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" + "'derby.database.defaultConnectionMode', 'noAccess')");
        // Confirming default connection mode
        rs = s.executeQuery ("VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY(" + "'derby.database.defaultConnectionMode')");
        rs.next();
        System.out.println("Value of defaultConnectionMode is " + rs.getString(1));

        // Defining full access usersusers
        s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" + "'derby.database.fullAccessUsers'," + un + ")");

        // Confirming full-access users
        rs = s.executeQuery("VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY(" + "'derby.database.fullAccessUsers')");
        rs.next();
        System.out.println("Value of fullAccessUsers is " + rs.getString(1));

        // Confirming read-only users
        rs = s.executeQuery("VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY(" + "'derby.database.readOnlyAccessUsers')");
        rs.next();
        System.out.println("Value of readOnlyAccessUsers is " + rs.getString(1));

        // We would set the following property to TRUE only
        // when we were ready to deploy.
        s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" + "'derby.database.propertiesOnly', 'false')");
        s.close();
    }
}
