package uk.ac.ebi.gxa.anatomogram.svgutil;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.geom.Point2D;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Olga Melnichuk
 *         Date: 03/03/2011
 */
public class SvgViewport {
    final private static Logger log = LoggerFactory.getLogger(SvgViewport.class);

    private static class ViewBox {
        private static final Pattern VIEWBOX_REGEXP = Pattern.compile("(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");

        final int minX, minY, width, height;

        private ViewBox(int minX, int minY, int width, int height) {
            this.minX = minX;
            this.minY = minY;
            this.width = width;
            this.height = height;
        }

        public static ViewBox parse(String str) {
            str = Strings.isNullOrEmpty(str) ? "" : str;
            Matcher m = VIEWBOX_REGEXP.matcher(str);
            if (m.matches()) {
                return new ViewBox(
                        Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3)),
                        Integer.parseInt(m.group(4))
                );
            }
            return null;
        }
    }

    private static class PreserveAspectRatio {
        private static final Pattern PAR_REGEXP = Pattern.compile("(none|x(:?Min|Mid|Max)Y(:?Min|Mid|Max)\\s+(:?meet|slice))");
        private static final String defaultValue = "xMidYMid meet";

        private final String value;

        private PreserveAspectRatio(String value) {
            this.value = value;
        }

        public static PreserveAspectRatio parse(String str) {
            str = Strings.isNullOrEmpty(str) ? "" : str;
            Matcher m = PAR_REGEXP.matcher(str);
            if (m.matches()) {
                return new PreserveAspectRatio(str);
            }
            return new PreserveAspectRatio(defaultValue);
        }

        public boolean isSlice() {
            return contains("slice");
        }

        public boolean isPreserveRatio() {
            return !value.equals("none");
        }

        public boolean alignMaxX() {
            return contains("xMax");
        }

        public boolean alignMidX() {
            return contains("xMid");
        }

        public boolean alignMaxY() {
            return contains("YMax");
        }

        public boolean alignMidY() {
            return contains("YMid");
        }

        private boolean contains(String str) {
            return value.indexOf(str) >= 0;
        }
    }

    private static class PixelLength {
        private static final Pattern PX_VALUE = Pattern.compile("(\\d+)(:?px)?");
        final int value;

        private PixelLength(int length) {
            this.value = length;
        }

        public static PixelLength parse(String str) {
            str = Strings.isNullOrEmpty(str) ? "" : str;
            Matcher m = PX_VALUE.matcher(str);
            if (m.matches()) {
                return new PixelLength(Integer.parseInt(m.group(1)));
            }
            return null;
        }
    }

    private final double sx, sy, tx, ty;

    public SvgViewport(double sx, double sy, double tx, double ty) {
        this.sx = sx;
        this.sy = sy;
        this.tx = tx;
        this.ty = ty;
    }

    public Point2D.Float fix(Point2D.Float point) {
        return new Point2D.Float((float) (point.x * sx + tx), (float) (point.y * sy + ty));
    }

    public static SvgViewport create(Document doc) {
        Element root = doc.getDocumentElement();

        ViewBox viewBox = ViewBox.parse(root.getAttribute("viewBox"));
        PreserveAspectRatio preserveAspectRatio = PreserveAspectRatio.parse(root.getAttribute("preventAspectRatio"));
        PixelLength width = PixelLength.parse(root.getAttribute("width"));
        PixelLength height = PixelLength.parse(root.getAttribute("height"));

        double sx = 1.0, sy = 1.0, tx = 0.0, ty = 0.0;
        if (width == null || height == null) {
            log.error("Non pixel length is not supported in svg tag");
            return new SvgViewport(sx, sy, tx, ty);
        }

        if (viewBox == null || preserveAspectRatio.isSlice()) {
            return new SvgViewport(sx, sy, tx, ty);
        }

        double newWidth = width.value * (1.0);
        double newHeight = height.value * (1.0);

        if (preserveAspectRatio.isPreserveRatio()) {
            double ratio1 = viewBox.width * (1.0) / viewBox.height;
            double ratio2 = width.value * (1.0) / height.value;

            if (viewBox.width == viewBox.height) {
                if (ratio2 > 1.0) { // w > h
                    newWidth = height.value;
                } else if (ratio2 < 1.0) { // w < h
                    newHeight = width.value;
                }
            } else if (ratio1 > 1.0) { // w > h
                newWidth = height.value * 1.0 * ratio1;
            } else if (ratio1 < 1.0) { // w < h
                newHeight = width.value * 1.0 / ratio1;
            }
        }

        if (preserveAspectRatio.alignMaxX()) {
            tx = width.value - newWidth;
        } else if (preserveAspectRatio.alignMidX()) {
            tx = (width.value - newWidth) / 2.0;
        }

        if (preserveAspectRatio.alignMaxY()) {
            ty = height.value - newHeight;
        } else if (preserveAspectRatio.alignMidY()) {
            ty = (height.value - newHeight) / 2.0;
        }

        sx = newWidth / viewBox.width;
        sy = newHeight / viewBox.height;

        return new SvgViewport(sx, sy, tx, ty);
    }
}
