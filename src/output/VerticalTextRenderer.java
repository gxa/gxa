package output;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRenderedImage;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.font.TextLayout;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: ostolop
 * Date: Apr 8, 2008
 * Time: 9:53:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class VerticalTextRenderer {
    protected final Log log = LogFactory.getLog(getClass());

    private static final Map hints = new HashMap();

    static {
        hints.put(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
        hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }

    public static WritableRenderedImage drawVerticalTextImage(final String s) {
        BufferedImage img = new BufferedImage(14, 130, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHints(hints);

        TextLayout layout = new TextLayout(
                s,
                g2d.getFont(),
                g2d.getFontRenderContext());

        g2d.translate(10, 128);
        g2d.rotate(- Math.PI / 2);
        g2d.setColor(Color.black);
        g2d.setPaint(Color.black);
        layout.draw(g2d, 0, 0);

        return img;
    }
}
