package authentication;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class FileSystemAuthenticator extends BasicAuthenticator {
	final private String dbPath;
	private Connection connection = null;
	final private int saltsize = 6;
	HashMap<Integer, Long> userList = new HashMap<Integer, Long>();

	public FileSystemAuthenticator(String arg0, String database) throws ClassNotFoundException {
		super(arg0);
		this.dbPath = database;
		// load the sqlite-JDBC driver using the current class loader
		Class.forName("org.sqlite.JDBC");
	    
	    try
	    {
	      // create a database connection
	      connection = DriverManager.getConnection("jdbc:sqlite:"+ dbPath);
	      Statement statement = connection.createStatement();
	      statement.setQueryTimeout(30);  // set timeout to 30 sec.

//	      statement.executeUpdate("drop table if exists person");
//	      statement.executeUpdate("create table person (id integer, name string)");
//	      statement.executeUpdate("insert into person values(1, 'leo')");
//	      statement.executeUpdate("insert into person values(2, 'yui')");
	      ResultSet rs = statement.executeQuery("select * from users");
	      for(int i = 1; i <= rs.getMetaData().getColumnCount(); i++){
	    	  String app = (i==rs.getMetaData().getColumnCount()) ? "" : "  |  ";
	    	  System.out.print(rs.getMetaData().getColumnLabel(i) + app);
	      }
	      System.out.println();
	      while(rs.next())
	      {
  	    	  try{
  	    	  if(rs.getInt("new") == 1){
  	    		  Statement upd = connection.createStatement();
  	    		  upd.setQueryTimeout(30);
  	    		  
  	    		  MessageDigest digest = MessageDigest.getInstance("SHA-256");
  	    		  Random rand = new Random();
  	    		  byte salt[] = new byte[saltsize];
  	    		  rand.nextBytes(salt);
  	    		  
  	    		  digest.update(salt, 0, salt.length);
  	    		  byte pbytes[] = rs.getString("passwd").getBytes();
  	    		  digest.update(pbytes, 0, pbytes.length);
  	    		  
  	    		  byte salted[] = digest.digest();
  	    		  upd.executeUpdate("UPDATE users"
  	    				  +" SET new=0,salt=\"" + Base64.encode(salt)
  	    				  +"\",passwd=\"" + Base64.encode(salted)
  	    				  +"\" WHERE id=" + rs.getString("id") + ";");
  	    	  }
  	    	  } catch( NoSuchAlgorithmException | SQLException e) {
  	    		  e.printStackTrace();
  	    	  }
  	    	  
	    	  for(int i = 1; i <= rs.getMetaData().getColumnCount(); i++){
	    		  String app = (i==rs.getMetaData().getColumnCount()) ? "" : "  |  ";
	    		  System.out.print(rs.getString(i) + app);
	    	  }
	    	  System.out.println();
	      }
	    }
	    catch(SQLException e)
	    {
	      // if the error message is "out of memory", 
	      // it probably means no database file is found
	      System.err.println(e.getMessage());
	    }
//	    finally
//	    {
//	      try
//	      {
//	        if(connection != null)
//	          connection.close();
//	      }
//	      catch(SQLException e)
//	      {
//	        // connection close failed.
//	        System.err.println(e);
//	      }
//	    }
	}
	
//	@Override
//	public Authenticator.Result authenticate(HttpExchange t){
//		super.authenticate(t);
//	}
	
	private long calltime = 0;
	
    @Override
    public boolean checkCredentials(String user, String pwd) {
    	calltime = System.currentTimeMillis();
    	try{
  	      Statement statement = connection.createStatement();
  	      statement.setQueryTimeout(30);  // set timeout to 30 sec.
  	      ResultSet users = statement.executeQuery("select * from users");
  	      HashMap<String, SimplePair<String,String>> userinfos = new HashMap<String, SimplePair<String,String>>();
  	      while(users.next()){
  	    	  userinfos.put(users.getString("name"), new SimplePair<String, String>(users.getString("passwd"), users.getString("salt")));
  	    	  
  	      }
  	      if(!userinfos.containsKey(user)) return returnVal(false,100);
  	      String userpwd = userinfos.get(user).key;
  	      String usersalt = userinfos.get(user).val;
  	      String workedPwd = null;
  	      try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte salt[] = Base64.decode(usersalt);
			digest.update(salt, 0, salt.length);
			digest.update(pwd.getBytes(), 0, pwd.getBytes().length);
			workedPwd = Base64.encode(digest.digest());
		} catch (NoSuchAlgorithmException | Base64DecodingException e) {
			e.printStackTrace();
		}
  	    return returnVal(userpwd.equals(workedPwd),100);
    	}	    
    	catch(SQLException e)
	    {
  	      // if the error message is "out of memory", 
  	      // it probably means no database file is found
  	      System.err.println(e.getMessage());
  	      return returnVal(false, 100);
  	    }
    }
    
    private boolean returnVal(boolean ret, long checkout){
    	while(System.currentTimeMillis() < calltime+checkout){
    		if(System.currentTimeMillis()-checkout > calltime+checkout-3){
	    		try {
					Thread.sleep(calltime+checkout-2 - System.currentTimeMillis());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
    	}
    	return ret;
    }
    
    private class SimplePair<K,V>{
    	K key;
    	V val;
    	SimplePair(K k, V v){
    		this.key = k;
    		this.val = v;
    	}
    }

}
