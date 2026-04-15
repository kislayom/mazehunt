<%-- 
    Document   : trending
    Created on : 13 Mar, 2016, 11:38:37 PM
    Author     : kislay
--%>

<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Statement"%>
<%@page import="com.utils.DbConnection"%>
<%@page import="java.sql.Connection"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>


<br>
<div class="row">
    <div class="col-md-1"></div>
    <div class="col-md-7 panel-body">
        <div class="panel panel-default">
            <div class="panel-heading"><strong>Top 0xA trending players: </strong></div>

            <div class="row">
                <div class="col-md-3">
                    <img src="images/fMSZhTq.png" width="200">
                </div>
                <div class="col-md-9">
                    <div class="panel-danger text-left">
                        <table class="table table-striped">
                            <tr>
                                <th>Rank</th>
                                <th>Email id</th>

                            </tr>
                           
                                <%
                                    Connection con = null;
                                    try {
                                        con = DbConnection.getConnection();
                                        Statement stmt = con.createStatement();
                                        String email="";
                                        ResultSet rs = stmt.executeQuery("select email_id, count(0), sum(unix_timestamp(explored_timestamp)) time1 from explored_nodes group by email_id order by count(0) desc,time1 asc limit 10");
                                        while (rs.next()) {
                                            email = rs.getString(1);
                                            out.write("<tr><td>"+rs.getRow()+"</td><td>"+email+"</td></tr>");
                                        } 

                                    } catch (Exception exc) {
                                        out.write(exc.toString());
                                    } finally {
                                        try{
                                        con.close();
                                        }catch(Exception exc){
                                            
                                        }
                                    }
                                %>
                                
                            
                        </table>


                        <br>
                       Competition is rising!!  <strong>Sherlock</strong> ;)
                        </p>
                    </div>
                </div>

            </div>
        </div>


    </div>

</div>


