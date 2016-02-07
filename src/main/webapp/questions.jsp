<%-- 
    Document   : questions
    Created on : 7 Feb, 2016, 1:45:41 PM
    Author     : kislay
--%>

<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Statement"%>
<%@page import="java.sql.Connection"%>
<%@page import="com.utils.DbConnection"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>
        <%
            String quesid = request.getParameter("quesid");
            String question="";
            if (quesid == null || quesid.length() == 0) {
                RequestDispatcher rd = request.getRequestDispatcher("index.jsp?error=Did you just try to hack up?.");
                session.setAttribute("loginstatus", "false");
                rd.forward(request, response);
                }
                Connection con = null;
                try {
                    con = DbConnection.getConnection();
                    Statement stmt=con.createStatement();
                    ResultSet rs=stmt.executeQuery("select question from question_info where id='"+quesid+"'");
                    if(rs.next()){
                        question=rs.getString(1);
                    }else{
                        question ="No question available";
                    }
                    
                } catch (Exception exc) {
                    out.write(exc.toString());
                } finally {
                    try {
                        con.close();
                    } catch (Exception exc) {

                    }
                }

            

        %>
        <br>
        <br>
        <div class="row">
            <div class="col-md-1"></div>
            <div class="col-md-7">
                <div class="panel panel-default">
                    <div class="panel-heading">
                        Now whats that ?
                    </div>
                    <div class="panel-info">
                        <%= question%>
                        
                        <form>
                            <div class="row">
                                <div class="col-md-3"></div>
                                <div class="col-md-5">
                                    <div class="form-group">

                                        <input type="text" class="form-control" id="answer" placeholder="Answer" name="answer">

                                    </div>
                                </div>
                            </div>
                            <div class="row">
                                <div class="col-md-4"></div>
                                <div class="col-md-3">
                                    <button type="submit" class="btn btn-danger">Bingo!!</button>
                                </div>
                            </div>
                        </form>
                    </div>
                    &nbsp;<br>
                </div>
            </div>
        </div>


    </body>
</html>
