<%@ page language="java" contentType="text/html; charset=ISO-8859-1"    pageEncoding="ISO-8859-1" %>
<%@ page import="javax.servlet.ServletOutputStream"  %>
<%@ page import="javax.servlet.ServletException"  %>
<%@ page import="javax.servlet.http.HttpServletResponse"  %>
<%@ page import="javax.servlet.http.HttpServlet"  %>
<%@page import="ae3.service.DownloadService"%>
<%@ page import="java.io.*"  %>

<%
	String qid = request.getParameter("qid");
	StringBuilder strBuf = DownloadService.getFileContent(session.getId(),qid);
	String filename = DownloadService.getDownloadFileName(session.getId(),qid);
	
	try {
		BufferedReader bufferedReader=null;
   	 	response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition","attachment;filename="+filename+".tab");
        //InputStream in = new ArrayInputStream(strBuf.toString().getBytes("UTF-8"));
        //File file = new File("/Volumes/Workspace/Projects/atlas-1.0/out.csv");
        //FileInputStream fileIn = new FileInputStream(file);
        bufferedReader = new BufferedReader(new StringReader(strBuf.toString()));
        	
     try{
			int anInt=0;
			while((anInt=bufferedReader.read())!=-1)
			out.write(anInt);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}

      }
      catch (Exception e) {
        System.err.println(e);
      }
%>