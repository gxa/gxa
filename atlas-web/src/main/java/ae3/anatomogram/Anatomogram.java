package ae3.anatomogram;

import org.apache.batik.dom.util.DOMUtilities;
import org.apache.batik.parser.PathHandler;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This code originally extracted from the Annotator.java...
 *
 * @author Olga Melnichuk
 *         Date: Dec 13, 2010
 */
public class Anatomogram {

    static class Annotation {
        private String id;
        private String caption;
        private int up;
        private int dn;
        private float x;
        private float y;

        public Annotation(String id, String caption, int up, int dn, float x, float y) {
            this.id = id;
            this.caption = caption;
            this.up = up;
            this.dn = dn;
            this.x = x;
            this.y = y;
        }
    }

    public enum Encoding {
        Svg, Jpeg, Png
    }

    enum HeatmapStyle {
        UpDn, Up, Dn, Blank;

        public static HeatmapStyle forUpDnValues(int up, int dn) {

            if ((up > 0) && (dn > 0)) {

                return UpDn;

            } else if (up > 0) {

                return Up;

            } else if (dn > 0) {

                return Dn;
            }

            return Blank;
        }
    }

    public static final int MAX_ANNOTATIONS = 9;

    private final Document svgDocument;
    private List<Annotation> annotations = new ArrayList<Annotation>();
    private List<AnatomogramArea> map = new ArrayList<AnatomogramArea>();

    public Anatomogram(Document svgDocument) {
        this.svgDocument = svgDocument;
    }

    public void writePngToStream(OutputStream outputStream) throws IOException, TranscoderException {
        writeToStream(Encoding.Png, outputStream);
    }

    public void writeToStream(Encoding encoding, OutputStream outputStream) throws IOException, TranscoderException {
        if (outputStream == null) {
            return;
        }

        switch (encoding) {
            case Svg: {
                DOMUtilities.writeDocument(svgDocument, new OutputStreamWriter(outputStream));
                break;
            }
            case Jpeg: {
                JPEGTranscoder t = new JPEGTranscoder();
                // t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(. 8));
                TranscoderInput input = new TranscoderInput(svgDocument);
                TranscoderOutput output = new TranscoderOutput(outputStream);
                t.transcode(input, output);
                break;
            }
            case Png: {
                PNGTranscoder t = new PNGTranscoder();
                //t.addTranscodingHint(JPEGTranscoder.KEY_WIDTH, new Float(350));
                //t.addTranscodingHint(JPEGTranscoder.KEY_HEIGHT, new Float(150));
                TranscoderInput input = new TranscoderInput(svgDocument);
                TranscoderOutput output = new TranscoderOutput(outputStream);
                t.transcode(input, output);
                break;
            }
            default:
                throw new IllegalStateException("unknown encoding");
        }
    }

    public List<AnatomogramArea> getAreaMap() {
        List<AnatomogramArea> list = new ArrayList<AnatomogramArea>();
        list.addAll(map);
        return list;
    }

    public void addAnnotation(String id, String caption, int up, int dn) {
        if (map.size() >= MAX_ANNOTATIONS) {
            return;
        }

        Element elem = svgDocument.getElementById(id);
        if (elem != null) {
            AnnotationPathHandler pathHandler = new AnnotationPathHandler();
            parseElement(elem, pathHandler);
            annotations.add(new Annotation(id, caption, up, dn, pathHandler.getCenterX(), pathHandler.getCenterY()));
            applyChanges();
        }
    }

    public boolean isEmpty() {
        return annotations.isEmpty();
    }

    private void applyChanges() {

        map.clear();

        Collections.sort(annotations, new Comparator<Annotation>() {
            public int compare(Annotation a1, Annotation a2) {
                return Float.compare(a1.y, a2.y);
            }
        });

        Editor editor = new Editor(svgDocument);

        for (int i = 1; i <= MAX_ANNOTATIONS; i++) {

            String index = formatInt(i);
            final String calloutId = "pathCallout" + index;
            final String rectId = "rectCallout" + index;
            final String triangleId = "triangleCallout" + index;
            final String textCalloutUpId = "textCalloutUp" + index;
            final String textCalloutDnId = "textCalloutDn" + index;
            final String textCalloutCenterId = "textCalloutCenter" + index;
            final String textCalloutCaptionId = "textCalloutCaption" + index;

            boolean noAnnotation = i >= annotations.size();
            String visibility = noAnnotation ? "hidden" : "visible";

            editor.setVisibility(calloutId, visibility);
            editor.setVisibility(rectId, visibility);
            editor.setVisibility(triangleId, visibility);
            editor.setVisibility(textCalloutUpId, visibility);
            editor.setVisibility(textCalloutDnId, visibility);
            editor.setVisibility(textCalloutCenterId, visibility);
            editor.setVisibility(textCalloutCaptionId, visibility);

            if (noAnnotation) {
                continue;
            }

            Element calloutEl = svgDocument.getElementById(calloutId);
            if (null == calloutEl)
                throw new IllegalStateException("can not find element" + calloutId);

            // NB. i-1 because while indexing in svg file starts from 1, java arrays are indexed from 0
            Annotation currAn = annotations.get(i-1);

            CalloutPathHandler calloutPathHandler = new CalloutPathHandler();
            parseElement(calloutEl, calloutPathHandler);

            final float X = calloutPathHandler.getRightmostX();
            final float Y = calloutPathHandler.getRightmostY();

            String calloutPath = String.format("M %f,%f L %f,%f"
                    , currAn.x
                    , currAn.y
                    , X
                    , Y);

            calloutEl.setAttributeNS(null, "d", calloutPath);

            final HeatmapStyle style = HeatmapStyle.forUpDnValues(currAn.up, currAn.dn);

            switch (style) {
                case UpDn:
                    editor.fill(rectId, "blue");
                    editor.fill(triangleId, "red");
                    editor.setTextAndAlign(textCalloutUpId, formatInt(currAn.up));
                    editor.setTextAndAlign(textCalloutDnId, formatInt(currAn.dn));
                    editor.setVisibility(textCalloutCenterId, "hidden");

                    editor.fill(currAn.id, "grey");
                    editor.setOpacity(currAn.id, "0.5");
                    break;
                case Up:
                    editor.fill(rectId, "red");
                    editor.setVisibility(triangleId, "hidden");
                    editor.setTextAndAlign(textCalloutCenterId, formatInt(currAn.up));
                    editor.setVisibility(textCalloutUpId, "hidden");
                    editor.setVisibility(textCalloutDnId, "hidden");

                    editor.fill(currAn.id, "red");
                    editor.setOpacity(currAn.id, "0.5");
                    break;
                case Dn:
                    editor.fill(rectId, "blue");
                    editor.setVisibility(triangleId, "hidden");
                    editor.setTextAndAlign(textCalloutCenterId, formatInt(currAn.dn));
                    editor.setVisibility(textCalloutUpId, "hidden");
                    editor.setVisibility(textCalloutDnId, "hidden");

                    editor.fill(currAn.id, "blue");
                    editor.setOpacity(currAn.id, "0.5");
                    break;
                case Blank:
                    editor.fill(rectId, "none");
                    editor.setVisibility(triangleId, "hidden");
                    editor.setText(textCalloutCenterId, formatInt(0));
                    editor.setVisibility(textCalloutUpId, "hidden");
                    editor.setVisibility(textCalloutDnId, "hidden");
                    editor.setStroke(textCalloutCenterId, "black");

                    editor.setOpacity(currAn.id, "0.5");
                    break;
            }

            editor.setText(textCalloutCaptionId, currAn.caption);

            Element rectEl = svgDocument.getElementById(rectId);
            Float x = Float.parseFloat(rectEl.getAttribute("x"));
            Float y = Float.parseFloat(rectEl.getAttribute("y"));
            Float height = Float.parseFloat(rectEl.getAttribute("height"));
            Float width = Float.parseFloat(rectEl.getAttribute("width"));

            AnatomogramArea area = new AnatomogramArea();
            area.x0 = x.intValue();
            area.x1 = Math.round(x + width + 200);
            area.y0 = y.intValue();
            area.y1 = Math.round(y + height);
            area.name = currAn.caption;
            area.efo = currAn.id;

            map.add(area);
        }
    }

    private void parseElement(Element elem, PathHandler pathHandler) {
        String s_efo0 = elem.getAttribute("d");
        org.apache.batik.parser.PathParser pa = new org.apache.batik.parser.PathParser();
        pa.setPathHandler(pathHandler);
        pa.parse(s_efo0);
    }

    private static String formatInt(int i) {
        return String.format("%1$d", i);
    }

}
