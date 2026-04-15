<%@ page import="java.util.*" %>
<%@ page import="com.utils.DbConnection" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<html>
    <head>
        <script language="javascript" type="text/javascript"  src="https://ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.min.js"></script>
        <script language="javascript" type="text/javascript" src="assets/arbor/lib/arbor.js" ></script>
        <script language="javascript" type="text/javascript" src="assets/arbor/demos/_/graphics.js" ></script>
        <script language="javascript" type="text/javascript" src="assets/arbor/demos/halfviz/src/renderer.js" ></script>



        <%

            String emailid = (String) session.getAttribute("username");
            Connection con = null;
            con = DbConnection.getConnection();
            Statement stmt = con.createStatement();
            List ques_attribute = new ArrayList();
            List child_info = new ArrayList();

            ResultSet rs2 = stmt.executeQuery(
                    "select ques_attribute,child_info from question_attribute where question_id in (select traverse_id from question_travel  where question_id in (select question_id from explored_nodes where email_id='" + emailid + "'))");

            while (rs2.next()) {
                ques_attribute.add(rs2.getString("ques_attribute"));
                child_info.add(rs2.getString("child_info"));
            }
        %>

    </head>
    <body>
        <div class="col-md-12 text-left">
            <span class="glyphicon glyphicon-home" aria-hidden="true"> <a href="gamecontainer.jsp">Home</a></span>
            <span class="glyphicon glyphicon-apple" aria-hidden="true"> <a href="#instruction" onclick="load('instruction'); return false;">Instruction Set</a></span>
            <span class="glyphicon glyphicon-apple" aria-hidden="true"> <a href="#trending" onclick="load('trending'); return false;">Trending Players</a></span>
            <span class="glyphicon glyphicon-home" aria-hidden="true"> <a href="logout.jsp">Logout</a></span>
        </div>

        <canvas id="viewport" width="300" height="800"></canvas>
        <script language="javascript" type="text/javascript">
            <%
                if (ques_attribute.size() > 0) {  %>

            var sys = arbor.ParticleSystem(1000, 650, 1,true,70);
            <% } else { %>

            sys = arbor.ParticleSystem(0, 0, 0);
            <% }%>
            sys.parameters({gravity:true});
            sys.renderer = Renderer("#viewport");
            var data = {
            nodes:{
            <%

                if (ques_attribute.size() > 0) {
                    for (int i = 0; i < ques_attribute.size(); i++) {
                        out.println(ques_attribute.get(i));
                    }
                } else {%>

            101:{'color':'red', 'shape':'dot', 'label':'question101'},
            <% }%>
            },
                    edges:{
            <%
                if (child_info.size() > 0) {
                    for (int i = 0; i < child_info.size(); i++) {
                        out.println(child_info.get(i));
                    }
                } else {   %>
                    101:{ },
            <%}%>
                    }
            };
            sys.graft(data);
        </script>
    </body>
</html>
