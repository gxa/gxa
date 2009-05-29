package ae3.servlet;

import output.DiagonalTextRenderer;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.WritableRenderedImage;
import java.util.List;
import java.util.ArrayList;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
        Integer[] depths = null;
        String sessionName = req.getParameter("st");
        if(sessionName != null) {
            HttpSession session = req.getSession(false);
            if(session != null) {
                Iterable iterable = (Iterable)session.getAttribute(sessionName);
                String textMethodName = req.getParameter("mt");
                String depthMethodName = req.getParameter("md");
                Method getTextMethod = null;
                Method getDepthMethod = null;
                List<String> textl = new ArrayList<String>();
                List<Integer> depthl = new ArrayList<Integer>();
                for(Object o : iterable) {
                    if(getTextMethod == null) {
                        Class klass= o.getClass();
                        try {
                            getTextMethod = klass.getMethod(textMethodName, (Class[]) null);
                        } catch(NoSuchMethodException e) {
                            break;
                        }
                        if(depthMethodName != null) {
                            try {
                                getDepthMethod = klass.getMethod(depthMethodName, (Class[]) null);
                            } catch (NoSuchMethodException e) {
                                // it's ok
                            }
                        }
                    }
                    try {
                        textl.add((String) getTextMethod.invoke(o, (Object[]) null));
                        if (getDepthMethod != null) {
                            depthl.add((Integer) getDepthMethod.invoke(o, (Object[]) null));
                        }
                    } catch(IllegalAccessException e) {
                        break;
                    } catch(InvocationTargetException e) {
                        break;
                    }
                }
                if(!textl.isEmpty())
                    texts = textl.toArray(new String[textl.size()]);
                if(!depthl.isEmpty())
                    depths = depthl.toArray(new Integer[depthl.size()]);
            }
        } else {
            texts = req.getParameterValues("t");
            String[] depthsStrs = req.getParameterValues("d");
            if(depthsStrs != null) {
                depths = new Integer[texts.length];
                for(int i = 0; i < depths.length; ++i) {
                    depths[i] = stoi(depthsStrs[i], 0);
                }
            }
        }

        if(texts == null) {
            res.setStatus(res.SC_NO_CONTENT);
            return;
        }

        res.setContentType("image/png");
        WritableRenderedImage img;
        if (depths == null) {
            img = DiagonalTextRenderer.drawTableHeader(texts,
                    stepWidth, maxHeight, fontSize, lineHeight,
                    textColor, lineColor);
        } else {
            int depthStep = stoi(req.getParameter("tds"), 4);
            int treeXShift = stoi(req.getParameter("tsx"), 7);
            int treeYShift = stoi(req.getParameter("tsy"), 5);
            Color treeColor = stoc(req.getParameter("tlc"), Color.black);

            img = DiagonalTextRenderer.drawTableTreeHeader(texts, depths, stepWidth, maxHeight, fontSize, lineHeight, 
                    depthStep, treeXShift, treeYShift, textColor, lineColor, treeColor);
        }
        ImageIO.write(img, "png", res.getOutputStream());
    }
}
