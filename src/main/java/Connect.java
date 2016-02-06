
import java.sql.*;
import javax.sql.*;

public class Connect
{
   public static void main (String[] args)
   {
       Connection conn = null;

       try
       {

           String url = "jdbc:mysql://192.168.1.102:3306/mazehunt";
           Class.forName ("com.mysql.jdbc.Driver");
           conn = DriverManager.getConnection (url,"amit","pathak");
           System.out.println ("Database connection established");
       }
       catch (Exception e)
       {
           e.printStackTrace();

       }
       finally
       {
           if (conn != null)
           {
               try
               {
                   conn.close ();
                   System.out.println ("Database connection terminated");
               }
               catch (Exception e) { /* ignore close errors */ }
           }
       }
   }
}

