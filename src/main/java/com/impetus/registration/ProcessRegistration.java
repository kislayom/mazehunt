/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.impetus.registration;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

import javax.mail.MessagingException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.impetus.DbPlan.MajeHuntDao;
import com.utils.EmailValidator;
import com.utils.RandomStringUtilsComm;
import com.utils.SendMail;

public class ProcessRegistration extends HttpServlet {
	MajeHuntDao majehuntdao =  new MajeHuntDao();

	private static final long serialVersionUID = 1L;

	/**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        boolean flag = false;
        try {
            /* TODO output your page here. You may use following sample code. */
            String emailid = request.getParameter("form-email");
            System.out.println("emailid>>>>>>>>>>>>>>>>>>>>>>"+emailid);
            String message = emailValidation(emailid);
            if("success".equalsIgnoreCase(message)){
            	String password = RandomStringUtilsComm.getRandomPassword();
            	System.out.println("password---"+password);
            	flag = majehuntdao.doRegistartion(emailid,password);
            	System.out.println("flag---"+flag);
            	if(flag){
            		out.println(emailid);
            		sendmailToUser(emailid,password);
            		RequestDispatcher rd=request.getRequestDispatcher("index.jsp?success=Registration Sucessfully.Kindly check mail for login credentials");
                    rd.forward(request, response);
            	} else {
            		RequestDispatcher rd=request.getRequestDispatcher("index.jsp?error=issue with registarion,Please try after some time");
                    rd.forward(request, response);
            	}
            } else {
      		  out.println(emailid);
      		  RequestDispatcher rd=request.getRequestDispatcher("index.jsp?error="+message+"");
              rd.forward(request, response);
      	}
        } catch (Exception exc) {
            out.println(exc.toString());
        } finally {
           
        }
    }
    
    private  String emailValidation(String emailid) throws ClassNotFoundException, SQLException{
    	String message = "";
    	if(EmailValidator.validateEmail(emailid)){
    		System.out.println("emailValidation");
    		if(majehuntdao.checkEmailId(emailid)){
    			message = "success";
    	  }  else {
    		  message = "EmailId is already exist in our system";  
    	  }
    	}else {
    		message = "Kindly provide valid email address,Please check it must belong to Impetus";
    	}
		return message;
    	
    }
    
    private void sendmailToUser(String emailid, String password) throws MessagingException{
    	String subject = "Mazehunt registration";
    	String message = "<html>\n <body>\n" +
        "\n" +
        " Thanks for registration <br>" +
        " you can use below credentials to start game <br><br><br>" +
        "<b>UserName</b> :: " + emailid + " <br>" +
        "<b>Password</b> :: " + password + " <br><br><br>" +
        " Thanks</b> <br>" +
        " MazeHunt Team</b>" +
        "</body>\n" +
        "</html>";
    	String sentFrom =  "mazehunt@gmail.com";
    	String recipient[] = {emailid};
    	SendMail.sendMail(
                recipient, subject, message, sentFrom);
    }
      
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
