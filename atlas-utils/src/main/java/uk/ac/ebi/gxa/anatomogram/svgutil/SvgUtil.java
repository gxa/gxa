package uk.ac.ebi.gxa.anatomogram.svgutil;

import org.apache.batik.parser.PathHandler;
import org.apache.batik.parser.PathParser;
import org.w3c.dom.Element;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SvgUtil {
    public static Point2D.Float getCenterPoint(Element element) {
        CenterWalker walker = new CenterWalker();
        parseElement(element, walker);
        return walker.getCenter();
    }

    public static Point2D.Float getRightmostPoint(Element element) {
        RightmostWalker walker = new RightmostWalker();
        parseElement(element, walker);
        return walker.getRightmost();
    }

    public static Collection<Point2D.Float> getArea(Element element) {
        final List<Point2D.Float> points = new ArrayList<Point2D.Float>();

        if ("rect".equalsIgnoreCase(element.getTagName())) {
            float x = Float.parseFloat(element.getAttribute("x"));
            float y = Float.parseFloat(element.getAttribute("y"));
            float width = Float.parseFloat(element.getAttribute("width"));
            float height = Float.parseFloat(element.getAttribute("height"));

            points.add(new Point2D.Float(x, y));
            points.add(new Point2D.Float(x + width, y));
            points.add(new Point2D.Float(x + width, y + height));
            points.add(new Point2D.Float(x, y + height));
            points.add(new Point2D.Float(x, y));
        } else if ("path".equalsIgnoreCase(element.getTagName())) {
            parseElement(element,  new PathWalker() {
                @Override
                protected void visit(float x, float y) {
                    points.add(new Point2D.Float(x,y));
                }
            });
        }
        return points;
    }

    private static void parseElement(Element element, PathHandler pathHandler) {
        String s_efo0 = element.getAttribute("d");
        PathParser pa = new PathParser();
        pa.setPathHandler(pathHandler);
        pa.parse(s_efo0);
    }
}
