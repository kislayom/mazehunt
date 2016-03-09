<html>
    <head>
        <script language="javascript" type="text/javascript"  src="https://ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.min.js"></script>
        <script language="javascript" type="text/javascript" src="assets/arbor/lib/arbor.js" ></script>
        <script language="javascript" type="text/javascript" src="assets/arbor/demos/_/graphics.js" ></script>
        <script language="javascript" type="text/javascript" src="assets/arbor/demos/halfviz/src/renderer.js" ></script>
        
        <%@ page import="java.util.*" %>
<%@ page import="com.utils.DbConnection" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.SQLException" %>
 
<%
Connection con = null;
con = DbConnection.getConnection();
Statement stmt = con.createStatement();              
%>
 
    </head>
    <body>
    <div class="col-md-12 text-left">
        <span class="glyphicon glyphicon-home" aria-hidden="true"> <a href="gamecontainer.jsp">Home</a></span>
        <span class="glyphicon glyphicon-apple" aria-hidden="true"> <a href="#instruction" onclick="load('instruction');return false;">Instruction Set</a></span>
    </div>

      <canvas id="viewport" width="300" height="800"></canvas>
      <script language="javascript" type="text/javascript">
            var sys = arbor.ParticleSystem(1000, 400,1);
            sys.parameters({gravity:true});
            sys.renderer = Renderer("#viewport") ;
           
           var data = {
               nodes:{
                  <% ResultSet rs = stmt.executeQuery(
                       "select ques_attribute,child_info from question_attribute where question_id in (101,102,103,104,105,106) ");
                   while(rs.next()) {
					    out.println(rs.getString("ques_attribute"));
				   }
              %>
               }, 
                edges:{
				  <% 
				  ResultSet rs1 = stmt.executeQuery(
                       "select child_info from question_attribute where question_id in (101,102,103,104,105,106) and child_info IS NOT NULL ");
				  while(rs1.next()) {
					 
					    out.println(rs1.getString("child_info"));
				   } %>
               }
             };
            sys.graft(data);
      </script>
    </body>
</html>
@RC1140
Owner
