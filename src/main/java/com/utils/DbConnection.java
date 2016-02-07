/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author kislay
 */
public class DbConnection {

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://104.168.163.65:3306/mazehunt";

    //  Database credentials
    static final String USER = "amit";
    static final String PASS = "pathak";

    public static Connection getConnection() throws ClassNotFoundException, SQLException {
        Connection conn = null;
        Class.forName("com.mysql.jdbc.Driver");

        //STEP 3: Open a connection
        System.out.println("Connecting to database...");
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
        return conn;
    }
    public static void main(String args[]) throws ClassNotFoundException, SQLException{
    	System.out.println(DbConnection.getConnection());
        System.out.println("connected");
    }
}
