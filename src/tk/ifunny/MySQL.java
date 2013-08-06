/*
 * iFunny let me use this. Thx.
 * 
 * Original source: https://github.com/FunnyJ2/fWarps/blob/master/src/tk/ifunny/mc/warp/db/MySQL.java
 * 
 * Say hi to him here:
 * 		GitHub: https://github.com/FunnyJ2
 * 		Twitter: https://twitter.com/FunnyJ2
 * 		Web: http://ifunny.tk/
 * 
 * Also send him bitcoins here: (He hasnt given me the address yet :()
 */
package tk.ifunny;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQL {

    private final String mySQLDatabase;
    private final String mySQLUsername;
    private final String mySQLPassword;
    private Connection connection;

    public MySQL(String database, String username, String password, String hostname) throws ClassNotFoundException, SQLException {
        this.mySQLDatabase = database;
        this.mySQLUsername = username;
        this.mySQLPassword = password;
        String url = "jdbc:mysql://" + hostname + ":" + "3306" + "/" + this.mySQLDatabase;
        Class.forName("com.mysql.jdbc.Driver");
        connection = DriverManager.getConnection(url + "?autoReconnect=true&user=" + this.mySQLUsername + "&password=" + this.mySQLPassword);
    }
    
    public ResultSet query(String query) {
		Statement statement;
		ResultSet result;
		try {
			statement = this.connection.createStatement();
			result = statement.executeQuery(query);
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
    
    public void manipulateData(String sql) {
    	Statement statement;
		try {
			statement = this.connection.createStatement();
			statement.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }
}