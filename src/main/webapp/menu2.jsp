<!DOCTYPE html>
<html>
<head>
<%@ page language="java" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import = "java.util.* "%> 
<%@ page import = "java.util.Map.Entry "%>


<%
HashMap<Integer, String[]> questions = new HashMap<Integer, String[]>();
    questions.put(1,new String[] {"101","102"});
    questions.put(2,new String[] {"101","102"});
    questions.put(101,new String[] {"106","107"});
%>
  <meta charset="utf-8">
  <title>jsTree test</title>
  <!-- 2 load the theme CSS file -->
  <link rel="stylesheet" href="assets/jstree/dist/themes/default/style.min.css" />
</head>
<body>
  <!-- 3 setup a container element -->
  <div class="row text-center">
    <div class="col-md-12 text-left">
        <span class="glyphicon glyphicon-home" aria-hidden="true"> <a href="gamecontainer.jsp">Home</a></span>
        <span class="glyphicon glyphicon-apple" aria-hidden="true"> <a href="#instruction" onclick="load('instruction');return false;">Instruction Set</a></span>
        <span class="glyphicon glyphicon-search" aria-hidden="true"> <a href="#instruction" onclick="load('questions', '101');return false;">Start Here</a></span>
    </div>
    
    <%
   for(Entry<Integer,String[]> entry: questions.entrySet()){
            System.out.println(entry.getKey());
          int id =entry.getKey(); %>
          <div class="row text-left">
            <span class="glyphicon glyphicon-search" aria-hidden="true"> <a href="#instruction" onclick="load('questions', <%=id%>);return false;"><%=id%></a></span>
 </div>
           <% for(String questionValue: entry.getValue()){%>
              <ul >
              <li><span class="glyphicon glyphicon-search" aria-hidden="true"> <a href="#instruction" onclick="load('questions', <%=questionValue%>);return false;"><%=questionValue%></a></span> </li>
             </ul>      
           
          
          <%   }
        }
    
    %>
</div>
  <!-- <div id="jstree">
    in this example the tree is populated from inline HTML 
    <ul>
      <li>Root node 1
        <ul>
          <li id="child_node_1">Child node 1</li>
          <li>Child node 2</li>
        </ul>
      </li>
      <li>Root node 2</li>
    </ul>
  </div>
  <button>demo button</button>-->

  <!-- 4 include the jQuery library -->
  <script src="assets/jstree/dist/lib/jquery.js"></script>
  <!-- 5 include the minified jstree source -->
  <script src="assets/jstree/dist/jstree.min.js"></script>
  <script>
  $(function () {
    // 6 create an instance when the DOM is ready
    $('#jstree').jstree();
    // 7 bind to events triggered on the tree
    $('#jstree').on("changed.jstree", function (e, data) {
      console.log(data.selected);
    });
  
  });
  </script>
</body>
</html>
