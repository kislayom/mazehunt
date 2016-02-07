<%-- 
    Document   : menu
    Created on : 7 Feb, 2016, 2:17:11 AM
    Author     : kislay
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<div class="row text-center">
    <div class="col-md-12 text-left">
        <span class="glyphicon glyphicon-home" aria-hidden="true"> <a href="gamecontainer.jsp">Home</a></span>
        <span class="glyphicon glyphicon-apple" aria-hidden="true"> <a href="#instruction" onclick="load('instruction');return false;">Instruction Set</a></span>
        <span class="glyphicon glyphicon-search" aria-hidden="true"> <a href="#instruction" onclick="load('questions', '101');return false;">Start Here</a></span>
    </div>
</div>
