package ae3.servlet;

import output.VerticalTextRenderer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.awt.image.WritableRenderedImage;

/**
 * Draws strings of vertical text using a special renderer.
 *
 * // TODO: cache the images.
 *  
 * User: ostolop
 * Date: Apr 16, 2008
 * Time: 2:51:33 PM
 */
public class VerticalTextServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String s = (String) req.getParameterNames().nextElement();

        res.setContentType("image/png");
        res.getOutputStream();

        if (s == null) s = "(no name)";

        WritableRenderedImage img = VerticalTextRenderer.drawVerticalTextImage(s);
        ImageIO.write(img, "png", res.getOutputStream());
    }
}
