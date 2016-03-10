<%-- 
    Document   : gamecontainer
    Created on : 7 Feb, 2016, 1:53:57 AM
    Author     : kislay
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>

<%
    String login = (String)session.getAttribute("loginstatus");
    if(login==null||!login.equals("true")){
         RequestDispatcher rd = request.getRequestDispatcher("index.jsp?error=Session timed out.");
         rd.forward(request, response);
    }
%>

<html>

    <head>

        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>MazeHunt Login &amp; Register</title>

        <!-- CSS -->
        <link rel="stylesheet" href="http://fonts.googleapis.com/css?family=Roboto:400,100,300,500">
        <link rel="stylesheet" href="assets/bootstrap/css/bootstrap.min.css">
        <link rel="stylesheet" href="assets/font-awesome/css/font-awesome.min.css">
        <link rel="stylesheet" href="assets/css/form-elements.css">
        <link rel="stylesheet" href="assets/css/style.css">

        <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
        <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
        <!--[if lt IE 9]>
            <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
            <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
        <![endif]-->

        <!-- Favicon and touch icons -->
        <link rel="shortcut icon" href="assets/ico/favicon.png">
        <link rel="apple-touch-icon-precomposed" sizes="144x144" href="assets/ico/apple-touch-icon-144-precomposed.png">
        <link rel="apple-touch-icon-precomposed" sizes="114x114" href="assets/ico/apple-touch-icon-114-precomposed.png">
        <link rel="apple-touch-icon-precomposed" sizes="72x72" href="assets/ico/apple-touch-icon-72-precomposed.png">
        <link rel="apple-touch-icon-precomposed" href="assets/ico/apple-touch-icon-57-precomposed.png">
        <link rel="stylesheet" href="assets/jstree/dist/themes/default/style.min.css" />
        

    </head>
    <body style="background-image: url('images/sherlock_wallpaper_1920x1200_02.jpg');">
        <script type="text/javascript">

            function load(name, param) {

                $("#mainbox").load(name + '.jsp?quesid=' + param);
                $("#mainbox").fadeIn('3000');
            }

        </script> 
        <div class="row">
            <div class="col-md-4">
                <img class="img-rounded" src="images/logo.png">
                <div class="top-content">Welcome <%= session.getAttribute("username")%></div>
            </div>
            <div class="page-header col-md-4">
                <h1 class="text-success text-info"><strong style="color:#f0ad4e;">Hunt on Sherlock .....</strong></h1>
            </div>
            <div class='col-lg-3'>

            </div>
        </div>
        <div class="inner-bg">

            <div class="row">
                <div class="col-md-2 " id="menucontainer">
                    <!-- 
                    -->  
                    <%@include file="menu.jsp" %> 
                </div>
                <div class="col-md-10" style="min-height: 500px;" id="mainbox">

                </div>
            </div>

        </div>
        
        <script src="assets/bootstrap/js/bootstrap.min.js"></script>
        <script src="assets/js/scripts.js"></script>
         <script src="assets/jstree/dist/jstree.min.js"></script>
         
		 
		   <link rel="stylesheet" href="assets/js/jquery-ui.css">
  <script src="assets/js/jquery-1.11.1.js"></script>
  <script src="assets/js/jquery-ui.js"></script>
  <link rel="stylesheet" href="/resources/demos/style.css">
		 
		 
    </body>
</html>
