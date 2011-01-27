/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.requesthandlers.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRenderedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * @author pashky
 */
@SuppressWarnings("unchecked")
public class DiagonalTextRenderer {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final Map hints = new HashMap();

    static {
        hints.put(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
        hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }

    public static WritableRenderedImage drawTableHeader(final String[] texts,
                                                        int stepWidth, int maxHeight, int fontSize, int lineHeight,
                                                        Color textColor, Color lineColor) {

        String[] txts = texts.clone();
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED);

        Font font = new Font("Default", Font.PLAIN, fontSize);

        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHints(hints);

        int maxW = 0;
        int maxH = 0;
        double cs = 0.707106781186548;
        for(int i = 0; i < txts.length; ++i) {
            int bw;
            while(true) {
                TextLayout layout = new TextLayout(txts[i], font, g2d.getFontRenderContext());
                bw = (int)Math.round(layout.getBounds().getWidth() * cs);
                if(maxHeight > 0 && bw > maxHeight - 20 && txts[i].length() > 3)
                    txts[i] = txts[i].replaceAll(".(\\.\\.\\.)?$", "...");
                else
                    break;
            }
            if(maxH < bw)
                maxH = bw;
            if(maxW  < stepWidth * i + bw)
                maxW = stepWidth * i + bw;
        }

        g2d.dispose();

        int width = maxW + stepWidth + 20;
        img = new BufferedImage(width, maxHeight, BufferedImage.TYPE_INT_ARGB);

        g2d = img.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2d.fillRect(0, 0, width, maxHeight);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));

        g2d.setRenderingHints(hints);
        g2d.setPaint(Color.black);
        g2d.setStroke(new BasicStroke(1.0f));

        int x = 0;
        for(int i = 0; i < texts.length; ++i) {
            TextLayout layout = new TextLayout(txts[i], font, g2d.getFontRenderContext());

            AffineTransform saveAT = g2d.getTransform();
            g2d.translate(x + 9, maxHeight - 5);
            g2d.rotate(- Math.PI / 4);

            g2d.setColor(Color.WHITE);
            g2d.fillRect((int)(layout.getBounds().getMinX()) - 4, (int)(layout.getBounds().getMinY()) - 4, (int)(layout.getBounds().getWidth()) + 8, (int)(layout.getBounds().getHeight()) + 8);

            g2d.setColor(textColor);
            layout.draw(g2d, 0, 0);
            g2d.setTransform(saveAT);

            if(lineHeight > 0) {
                g2d.setColor(lineColor);
                g2d.drawLine(x, maxHeight - lineHeight, x, maxHeight);
                if(i > 0 ) {
                    int shift = maxHeight - lineHeight;
                    if(x + shift > texts.length * stepWidth)
                        shift = texts.length * stepWidth - x;
                    g2d.drawLine(x, maxHeight - lineHeight, x + shift, maxHeight - lineHeight - shift);
                }
            }

            x += stepWidth;
        }
        if(lineHeight > 0) {
            g2d.setColor(lineColor);
            g2d.drawLine(x, maxHeight - lineHeight, x, maxHeight);
        }

        g2d.dispose();

        return img;
    }

    public static WritableRenderedImage drawTableTreeHeader(
            final String[] texts, final Integer[] depths, final Boolean[] isExpandables,
            int stepWidth, int maxHeight, int fontSize, int lineHeight,
            int depthStep, int treeXShift, int treeYShift,
            Color textColor, Color lineColor, java.awt.Color treeLineColor) {

        String[] txts = texts.clone();
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED);

        Font font = new Font("Default", Font.PLAIN, fontSize);

        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHints(hints);

        int maxW = 0;
        int maxH = 0;
        double cs = 0.707106781186548;
        int maxDepth = -1;
        int maxTextHeight = maxHeight;
        for(int i = 0; i < txts.length; ++i) {
            if(depths[i] > maxDepth)
                maxDepth = depths[i];
        }

        maxTextHeight -= maxDepth * depthStep + depthStep + treeYShift;

        for(int i = 0; i < txts.length; ++i) {
            int bw;
            while(true) {
                TextLayout layout = new TextLayout(txts[i], font, g2d.getFontRenderContext());
                bw = (int)Math.round(layout.getBounds().getWidth() * cs);
                if(maxTextHeight > 0 && bw > maxTextHeight - 20 && txts[i].length() > 3)
                    txts[i] = txts[i].replaceAll(".(\\.\\.\\.)?$", "...");
                else
                    break;
            }
            if(maxH < bw)
                maxH = bw;
            if(maxW  < stepWidth * i + bw)
                maxW = stepWidth * i + bw;
        }

        g2d.dispose();

        int width = maxW + stepWidth + 20;
        int height = maxTextHeight;
        img = new BufferedImage(width, maxHeight, BufferedImage.TYPE_INT_ARGB);

        g2d = img.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2d.fillRect(0, 0, width, height);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));

        g2d.setRenderingHints(hints);
        g2d.setPaint(Color.black);
        g2d.setStroke(new BasicStroke(1.0f));


        Stack<Integer> starts = new Stack<Integer>();
        int currentDepth = -1;
        int x = 0;
        for(int i = 0; i < texts.length; ++i) {
            TextLayout layout = new TextLayout(txts[i], font, g2d.getFontRenderContext());

            AffineTransform saveAT = g2d.getTransform();
            g2d.translate(x + 9, height - 5);
            g2d.rotate(- Math.PI / 4);

            g2d.setColor(Color.WHITE);
            g2d.fillRect((int) (layout.getBounds().getMinX()) - 4, (int) (layout.getBounds().getMinY()) - 4, (int) (layout.getBounds().getWidth()) + 8, (int) (layout.getBounds().getHeight()) + 8);

            g2d.setColor(textColor);
            layout.draw(g2d, 0, 0);
            g2d.setTransform(saveAT);

            if(lineHeight > 0) {
                g2d.setColor(lineColor);
                g2d.drawLine(x, height - lineHeight, x, maxHeight);
                if(i > 0 ) {
                    int shift = height - lineHeight;
                    if(x + shift > texts.length * stepWidth)
                        shift = texts.length * stepWidth - x;
                    g2d.drawLine(x, height - lineHeight, x + shift, height - lineHeight - shift);
                }
            }

            g2d.setColor(treeLineColor);

            int depth = depths[i];
            int y = treeYShift + depthStep + height + (maxDepth - depth) * depthStep;
            if (depth > 0) {
                g2d.drawLine(x + treeXShift, y, x + treeXShift, y - depthStep);
            }

            if (isExpandables[i] != null && isExpandables[i].equals(Boolean.TRUE)) {
                g2d.drawRect(x + treeXShift - 3, y - depthStep - 5, 6, 6);
                g2d.drawLine(x + treeXShift - 3, y - depthStep - 2, x + treeXShift + 3, y - depthStep - 2);
                g2d.drawLine(x + treeXShift, y - depthStep - 5, x + treeXShift, y - depthStep + 1);
            } else {
                g2d.drawRect(x + treeXShift - 1, y - depthStep - 1, 2, 2);
            }

            for(y = y - depthStep - 3; y >= height; y -= 2)
                g2d.drawLine(x + treeXShift, y, x + treeXShift, y);

            if(depth > currentDepth) {
                starts.push(x);
                currentDepth = depth;
            } else {
                int currx = x;
                while(depth < currentDepth) {
                    int startx = starts.pop();
                    y = treeYShift + depthStep + height + (maxDepth - currentDepth) * depthStep;
                    g2d.drawLine(startx - stepWidth + treeXShift, y, currx - stepWidth + treeXShift, y);
                    currentDepth--;
                    currx = startx;
                }
            }

            x += stepWidth;
        }

        int currx = x;
        while (currentDepth != 0) {
            int startx = starts.pop();
            int y = treeYShift + depthStep + height + (maxDepth - currentDepth) * depthStep;
            g2d.drawLine(startx - stepWidth + treeXShift, y, currx - stepWidth + treeXShift, y);
            currentDepth--;
            currx = startx;
        }

        g2d.setColor(lineColor);

        if(lineHeight > 0) {
            g2d.setColor(lineColor);
            g2d.drawLine(x, height - lineHeight, x, height);
        }

        g2d.dispose();

        return img;
    }

}