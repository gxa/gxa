package ae3.servlet;

import output.DiagonalTextRenderer;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.WritableRenderedImage;
import java.awt.*;
import java.io.IOException;

/**
 * @author pashky
 */
public class TableHeaderServlet extends HttpServlet {
    private static int stoi(String str, int def) {
        try {
            return Integer.valueOf(str);
        } catch(Exception e) {
            return def;
        }
    }
    private static Color stoc(String str, Color def) {
        try {
            return Color.decode(str);
        } catch(Exception e) {
            return def;
        }
    }
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        int stepWidth = stoi(req.getParameter("s"), 26);
        int maxHeight = stoi(req.getParameter("h"), -1);
        int fontSize = stoi(req.getParameter("fs"), 11);
        int lineHeight = stoi(req.getParameter("lh"), 15);
        Color textColor = stoc(req.getParameter("tc"), Color.BLACK);
        Color lineColor = stoc(req.getParameter("lc"), Color.BLACK);

        String[] texts = null;
        if(req.getParameter("st") != null) {
            HttpSession session = req.getSession(false);
            if(session != null) {
                texts = (String[])session.getAttribute("diagonalTexts");
            }
        } else {
            texts = req.getParameterValues("t");
        }
        if(texts == null)
            texts = new String[0];

        res.setContentType("image/png");
        WritableRenderedImage img = DiagonalTextRenderer.drawTableHeader(texts,
                stepWidth, maxHeight, fontSize, lineHeight,
                textColor, lineColor);
        ImageIO.write(img, "png", res.getOutputStream());
    }
}
