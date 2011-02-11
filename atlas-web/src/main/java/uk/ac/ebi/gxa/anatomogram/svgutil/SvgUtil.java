package uk.ac.ebi.gxa.anatomogram.svgutil;

import org.apache.batik.parser.PathHandler;
import org.apache.batik.parser.PathParser;
import org.w3c.dom.Element;

import java.awt.geom.Point2D;

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

    private static void parseElement(Element element, PathHandler pathHandler) {
        String s_efo0 = element.getAttribute("d");
        PathParser pa = new PathParser();
        pa.setPathHandler(pathHandler);
        pa.parse(s_efo0);
    }
}
