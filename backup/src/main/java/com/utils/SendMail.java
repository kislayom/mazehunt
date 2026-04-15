/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.utils;


import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class SendMail {

    private static String SMTP_HOST_NAME = "smtp.gmail.com";
    private static String SMTP_PORT = "587";
    private static String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

    public static String sendMail(String recipients[], String subject, String message, String sentfrom) throws MessagingException {
        boolean debug = true;
        String sendStatus = null;
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST_NAME);
        props.put("mail.smtp.auth", "true");
        props.put("mail.debug", "true");
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.oyazen.socketFactory.class", SSL_FACTORY);
        props.put("mail.smtp.starttls.enable", "true");
        // This block is used to create session event
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("mazehunt@gmail.com", "0jb1a11a");
            }
        });

        session.setDebug(debug);
        Message msg = new MimeMessage(session);
        InternetAddress addressFrom = new InternetAddress(sentfrom);
        msg.setFrom(addressFrom);

        InternetAddress[] addressTo = new InternetAddress[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            addressTo[i] = new InternetAddress(recipients[i]);
        }
        msg.setRecipients(Message.RecipientType.TO, addressTo);
        msg.setSubject(subject);

        Multipart multipart = new MimeMultipart();
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(message, "text/html");
        multipart.addBodyPart(htmlPart);

        msg.setContent(multipart);
        try {
            Transport.send(msg);
            sendStatus = "Mail Succes";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sendStatus;
    }

    /**
     * Main method
     *
     */
    public static void main(String args[]) {
        String[] recipient = {"pathak.amit783@gmail.com"};
        String sentfrom = "mazehunt@gmail.com";
        String subject = "ffffffffffffffffffff";
        String message = "hello PFA";
        try {
            String result = sendMail(
                    recipient, subject, message, sentfrom);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
