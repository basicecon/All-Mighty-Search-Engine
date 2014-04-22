/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

/**
 *
 * @author Mengyao Wang
 */


import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import util.HTMLFilter;
import java.net.*;
import java.sql.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class Kookle extends HttpServlet {

    private static Connection connection = null;

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");

        out.println("<title>" + "Kookle" + "</title>");
        out.println("</head>");
        out.println("<body bgcolor=\"white\">");
/*
        out.println("<img src=\"../images/code.gif\" height=24 " +
                    "width=24 align=right border=0 alt=\"view code\"></a>");
        out.println("<a href=\"../index.html\">");
        out.println("<img src=\"../images/return.gif\" height=24 " +
                    "width=24 align=right border=0 alt=\"return\"></a>");
*/
        out.println("<h3 style=\"text-align: center\">" + "Kookle" + "</h3>");
        out.println("<div align=\"center\">");
        out.println("<img align=\"middle\" src=\"http://i.imgur.com/HZfbZHs.jpg\" alt=\"\"  width=\"700\" height=\"210\">");
        out.println("</div>");
        out.print("<form style=\"text-align: center\" action=\"");
        out.print("/\" ");
        out.println("method=POST>");
        out.println("<input type=text size=50 name=query>");
        out.println("<br>");
        out.println("<input type=submit>");
        out.println("</form>");



        String query = request.getParameter("query");
        out.println("<p>");
        out.println("<br>");
        if (query != null) {
            connectDB(query, out);
            //out.println("ur query is");
            //out.println(" = " + query + "<br>");

        } else {
        }
        out.println("</p>");
        out.println("</body>");
        out.println("</html>");
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);   
    }

    public void connectDB(String query, PrintWriter out) {
        try {
            if (connection == null) {
                Class.forName("com.mysql.jdbc.Driver");
                String url = "jdbc:mysql://localhost:3306/URLs";
                String username = "root";
                String password = "coatie013";

                connection = DriverManager.getConnection(url, username, password);   
            }
            
            processWord(query, out);
        } catch(Exception e) {
            out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void getURL(int urlid, PrintWriter out) {
        try {
            Statement stat = connection.createStatement();
            ResultSet result = stat.executeQuery("SELECT * FROM urls WHERE urlid = " + urlid + ";");
            if (result.next()) {
                String url = result.getString("url");
                //Document doc = Jsoup.connect(url).get();
                out.println("<font size=\"4\"><a href=\"" + url + "\">" + result.getString("title") + "</a></font>");
                out.println("<br>");
                out.println("<font color=\"green\">" + url + "</font>");
                out.println("<br>");
                out.println(result.getString("description"));
                out.println("<br>");
                out.println("<br>");
                out.println("<br>");
            }
        } catch(Exception e) {}
    }

    public void processWord(String query, PrintWriter out) {
        try {
            HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
            String[] words = query.toLowerCase().split(" ");
            int wordCnt = 0;
            for (int i = 0; i < words.length; i ++) {
                if (!words[i].equals("")) {
                    wordCnt ++;
                    Statement stat = connection.createStatement();
                    ResultSet result = stat.executeQuery("SELECT * FROM keyword WHERE word = '" + words[i] + "';"); 
                    while (result.next()) {
                        int tmpId = Integer.parseInt(result.getString("urlid"));
                        if (map.containsKey(tmpId)) {
                            map.put(tmpId, map.get(tmpId) + 1);
                        } else {
                            map.put(tmpId, 1);
                        }
                    }
                }
            }
            for (Integer id : map.keySet()) {
                if (map.containsKey(id)) {
                    if (map.get(id) == wordCnt) {
                        getURL(id, out);
                    }
                }
            }
        } catch(Exception e) {}
    }

}



