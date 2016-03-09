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
            String question = "";
            if (quesid == null || quesid.length() == 0) {
                RequestDispatcher rd = request.getRequestDispatcher("index.jsp?error=Did you just try to hack up?.");
                session.setAttribute("loginstatus", "false");
                rd.forward(request, response);
            }
            Connection con = null;
            try {
                con = DbConnection.getConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery("select question from question_info where id='" + quesid + "'");
                if (rs.next()) {
                    question = rs.getString(1);
                } else {
                    question = "No question available";
                }

            } catch (Exception exc) {
                out.write(exc.toString());
            } finally {

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

                                        <input id="answer1" type="text" class="form-control" id="answer" placeholder="Answer" name="answer">

                                    </div>
                                </div>
                            </div>
                            <div class="row">
                                <div class="col-md-4"></div>
                                <div class="col-md-3">
                                    <%
                                        try {
                                            Statement stmt = con.createStatement();
                                            String query = "select count(0) from explored_nodes where question_id='" + quesid + "' and email_id='" + session.getAttribute("username") + "'";
                                            ResultSet rs = stmt.executeQuery(query);
                                            if (rs.next()) {
                                                if (rs.getInt(1) == 0) {
                                                    out.write("<button type=\"button\" id=\"submitanswer\" class=\"btn btn-danger\">Bingo!!</button>");
                                                }

                                            }
                                        } catch (Exception exc) {

                                        } finally {
                                            try {
                                                con.close();
                                            } catch (Exception exc) {

                                            }
                                        }
                                    %>

                                    <script type="text/javascript">
                                        $(function () {

                                            $("#submitanswer").click(function () {
                                                var ans = $('#answer1').val();
                                                if (ans == '') {
                                                    $("#dialogmessage1").dialog("open");
                                                    alert('cant have empty answer');
                                                } else {
                                                    $.ajax({url: "QuestionController?data=" + ans + "&quesid=<%=quesid%>", success: function (result) {
                                                            if (result == 'Hare Krishna') {
                                                                load('success');
                                                                loadPageMenu();
                                                            } else {
                                                                alert('Try again Mr Williams!!');
                                                            }
                                                        }});

                                                }

                                            });
                                        });
                                        function loadPageMenu() {
                                            $('#menucontainer').load('menu.jsp');
                                        }
                                    </script>
                                </div>
                            </div>
                        </form>
                    </div>
                    &nbsp;<br>
                </div>
            </div>
        </div>
        <script type="text/javascript">
            $(function () {
                $("#dialogmessage1").dialog({
                    autoOpen: false,
                    show: {
                        effect: "blind",
                        duration: 1000
                    },
                    hide: {
                        effect: "explode",
                        duration: 1000
                    }
                });

                $("#answer1").click(function () {
                    $("#dialogmessage1").dialog("open");
                });
            });
        </script>

        <div id="dialogmessage1" title="Cannot have empty answer">
            <p>
                <span class="ui-icon ui-icon-circle-check" style="float:left; margin:0 7px 50px 0;"></span>
                Sherlock seems to be sleeping... 
            </p>
            <p>
                Whats up buddy ??.
            </p>
        </div>


    </body>
</html>
