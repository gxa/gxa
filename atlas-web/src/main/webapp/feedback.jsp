<%@ page import="java.util.Properties" %>
<%@ page import="javax.mail.Session" %>
<%@ page import="javax.mail.Message" %>
<%@ page import="javax.mail.internet.InternetAddress" %>
<%@ page import="javax.mail.internet.MimeMessage" %>
<%@ page import="javax.mail.Transport" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    try {
        boolean debug = false;

         //Set the host smtp address
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.ebi.ac.uk");

        // create some properties and get the default Session
        Session smtpSession = Session.getDefaultInstance(props, null);
        smtpSession.setDebug(debug);

        // create a message
        Message msg = new MimeMessage(smtpSession);

        // set the from and to address
        InternetAddress addressFrom = new InternetAddress("arrayexpress-atlas@ebi.ac.uk");
        msg.setFrom(addressFrom);
        msg.setRecipients(Message.RecipientType.TO, new InternetAddress[] { new InternetAddress("ostolop@ebi.ac.uk") });

        String email = request.getParameter("e");
        if (null != email && !email.equals(""))
            msg.setReplyTo(new InternetAddress[] {new InternetAddress(request.getParameter("e"))});

        // Setting the Subject and Content Type
        msg.setSubject("ArrayExpress Atlas Feedback");
        msg.setContent(request.getParameter("f"), "text/plain");

        Transport.send(msg);
        response.getWriter().write("SEND OK");
    } catch (Exception e) {
        response.getWriter().write("SEND FAIL");
    }
%>