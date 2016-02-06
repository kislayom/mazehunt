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
//	ProcessRegistration processregistration = new ProcessRegistration();
    /**
	 * 
	 */
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
            if(emailValidation(emailid)){
            	String password = RandomStringUtilsComm.getRandomPassword();
            	System.out.println("password---"+password);
            	flag = majehuntdao.doRegistartion(emailid,password);
            	if(flag){
            		sendmailToUser(emailid,password);
            	}
            }
            
         
            out.println(emailid);
        } catch (Exception exc) {
            out.println(exc.toString());
        } finally {
           
        }
    }
    
    private  boolean emailValidation(String emailid) throws ClassNotFoundException, SQLException{
    	boolean flag = false;
    	if(EmailValidator.validateEmail(emailid)){
    		System.out.println("emailValidation");
    		flag =majehuntdao.checkEmailId(emailid);
    		System.out.println("flag---"+flag);
    	}
		return flag;
    	
    }
    
    private void sendmailToUser(String emailid, String password) throws MessagingException{
    	String subject = "Mazehunt registration";
    	String message = "Thanks for registarion, you can use below credentails to start game" +
    			" UserName = " + emailid + "" +
    			" Password : " + password + "";
    	String sentFrom =  "mazehunt@gmail.com";
    	String recipient[] = {emailid};
    	SendMail.sendMail(
                recipient, subject, message, sentFrom);
    }
      
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
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
