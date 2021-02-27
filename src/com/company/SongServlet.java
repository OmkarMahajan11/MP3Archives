package com.company;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;

public class SongServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (Connection conn = DriverManager.getConnection("jdbc:h2:~/mydatabase")) {

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("select * from SONGS");

            while (rs.next()) {
                builder.append("<tr class=\"table\">")
                        .append("<td>").append(rs.getString("year")).append("</td>")
                        .append("<td>").append(rs.getString("artist")).append("</td>")
                        .append("<td>").append(rs.getString("album")).append("</td>")
                        .append("<td>").append(rs.getString("title")).append("</td>")
                        .append("</tr>");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String string = "<html><h1>Your Songs</h1><table><tr><th>Year</th><th>Artist</th><th>Album</th><th>Title</th></tr>" + builder.toString() + "</table></html>";
        resp.getWriter().write(string);
    }
}
