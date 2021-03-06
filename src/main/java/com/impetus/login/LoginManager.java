package com.impetus.login;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import com.impetus.DbPlan.MajeHuntDao;
import com.utils.DbConnection;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author kislay
 */
@WebServlet(name = "LoginManagerMain", urlPatterns = {"/LoginManagerMain"})
public class LoginManager extends HttpServlet {

    MajeHuntDao dao = new MajeHuntDao();

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
        Connection con = null;
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(true);
        try {

            /* TODO output your page here. You may use following sample code. */
            String emailid = request.getParameter("form-username");
            String password = request.getParameter("form-password");
            if (dao.doLogin(emailid, password)) {
                session.setAttribute("loginstatus", "true");
                session.setAttribute("username", emailid);
                RequestDispatcher rd = request.getRequestDispatcher("gamecontainer.jsp");
                rd.forward(request, response);
            } else {
                session.setAttribute("loginstatus", "false");
                RequestDispatcher rd = request.getRequestDispatcher("index.jsp?error=Login Failed !! Please try again.");
                rd.forward(request, response);
            }

        }catch(Exception exc){
            
        }
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
