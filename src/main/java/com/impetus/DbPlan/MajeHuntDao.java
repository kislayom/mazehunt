package com.impetus.DbPlan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.utils.DbConnection;
import javax.servlet.RequestDispatcher;

public class MajeHuntDao {

    public boolean checkEmailId(String emailid) throws ClassNotFoundException, SQLException {
        Connection con = null;
        con = DbConnection.getConnection();
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(
                "select count(*) from user_table where email_id='" + emailid + "' ");
        rs.next();
        int count = rs.getInt(1);
        if (count == 1) {
            return false;
        } else {
            return true;
        }
    }

    public boolean doLogin(String emailid, String password) {
        Connection con = null;
        try {
            con = DbConnection.getConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("select count(0) from user_table where email_id='" + emailid + "' and password='" + password + "' ");
            rs.next();
            int count = rs.getInt(1);
            if (count == 1) {
                return true;
            } else {
                return false;
            }
        } catch (Exception exc) {
            try {
                con.close();
            } catch (Exception exc1) {

            }
        }
        return false;
    }

    public boolean doRegistartion(String emailid, String password) throws SQLException {
        Connection con = null;
        PreparedStatement preparedStatement = null;
        boolean flag = false;
        try {
            System.out.println("Inside doRegistartion email " + emailid + "password " + password);
            con = DbConnection.getConnection();

            String sql = "insert into user_table values(?,?)";
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, emailid);
            preparedStatement.setString(2, password);
            preparedStatement.executeUpdate();
           
            flag = true;
        } catch (ClassNotFoundException | SQLException e) {

            System.out.println(e.getMessage());

        } finally {

            if (preparedStatement != null) {
                preparedStatement.close();
            }

            if (con != null) {
                con.close();
            }

        }
        return flag;

    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException {
        MajeHuntDao m = new MajeHuntDao();
        m.doRegistartion("kislay.osho@gmail.comgf", "fgfhg");
    }
}
