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

package uk.ac.ebi.gxa.anatomogram;

import org.apache.batik.transcoder.TranscoderException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.ac.ebi.gxa.anatomogram.svgutil.ImageFormat;
import uk.ac.ebi.gxa.anatomogram.svgutil.SvgUtil;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static uk.ac.ebi.gxa.anatomogram.svgutil.SvgUtil.getCenterPoint;

public class Anatomogram {

    static class OrganismPart implements Comparable<OrganismPart> {
        final String id;
        final String caption;
        final int up;
        final int dn;
        final int total;

        public OrganismPart(String id, String caption, int up, int dn) {
            this.id = id;
            this.caption = caption;
            this.up = up;
            this.dn = dn;
            total = up + dn;
        }

        /**
         * The more experiments, the more interesting the annotation is
         *
         * @param o the {@link uk.ac.ebi.gxa.anatomogram.Anatomogram.OrganismPart} to compare with
         * @return a negative integer, zero, or a positive integer as this object
         *         is less than, equal to, or greater than the specified object.
         */
        public int compareTo(OrganismPart o) {
            return -(total - o.total);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OrganismPart that = (OrganismPart) o;

            if (total != that.total) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return total;
        }
    }

    enum HeatmapStyle {
        UpDn, Up, Dn, Blank;

        public static HeatmapStyle forUpDnValues(int up, int dn) {
            if (up > 0 && dn > 0)
                return UpDn;

            if (up > 0)
                return Up;

            if (dn > 0)
                return Dn;

            return Blank;
        }
    }

    public static final int MAX_STRINGS_IN_LEGEND = 9;

    private final Document svgDocument;
    private List<OrganismPart> organismParts = new ArrayList<OrganismPart>();

    public Anatomogram(Document svgDocument) {
        this.svgDocument = svgDocument;
    }

    public void writePngToStream(OutputStream outputStream) throws IOException, TranscoderException {
        writeToStream(ImageFormat.PNG, outputStream);
    }

    public void writeToStream(ImageFormat encoding, OutputStream outputStream) throws IOException, TranscoderException {
        if (outputStream != null) {
            encoding.writeSvg(svgDocument, outputStream);
        }
    }

    void addOrganismParts(Collection<OrganismPart> parts) {
        organismParts.addAll(parts);
        prepareDocument();
    }

    public boolean isEmpty() {
        return organismParts.isEmpty();
    }

    private void prepareDocument() {
        leaveBest();

        Collections.sort(organismParts, new Comparator<OrganismPart>() {
            public int compare(OrganismPart a1, OrganismPart a2) {
                final Point2D.Float c1 = getCenterPoint(svgDocument.getElementById(a1.id));
                final Point2D.Float c2 = getCenterPoint(svgDocument.getElementById(a2.id));
                return Float.compare(c1.y, c2.y);
            }
        });

        Editor editor = new Editor(svgDocument);

        for (int i = 1; i <= MAX_STRINGS_IN_LEGEND; i++) {
            final String calloutId = "pathCallout" + i;
            final String rectId = "rectCallout" + i;
            final String triangleId = "triangleCallout" + i;
            final String textCalloutUpId = "textCalloutUp" + i;
            final String textCalloutDnId = "textCalloutDn" + i;
            final String textCalloutCenterId = "textCalloutCenter" + i;
            final String textCalloutCaptionId = "textCalloutCaption" + i;

            boolean noData = i > organismParts.size();
            String visibility = noData ? "hidden" : "visible";

            editor.setVisibility(calloutId, visibility);
            editor.setVisibility(rectId, visibility);
            editor.setVisibility(triangleId, visibility);
            editor.setVisibility(textCalloutUpId, visibility);
            editor.setVisibility(textCalloutDnId, visibility);
            editor.setVisibility(textCalloutCenterId, visibility);
            editor.setVisibility(textCalloutCaptionId, visibility);

            if (noData) {
                continue;
            }

            // NB. i-1 because while indexing in svg file starts from 1, java arrays are indexed from 0
            OrganismPart organismPart = organismParts.get(i - 1);

            Element calloutEl = svgDocument.getElementById(calloutId);
            if (null == calloutEl)
                throw new IllegalStateException("can not find element" + calloutId);

            Point2D.Float rightmost = SvgUtil.getRightmostPoint(calloutEl);
            Point2D.Float center = SvgUtil.getCenterPoint(svgDocument.getElementById(organismPart.id));

            String calloutPath = String.format("M %f,%f L %f,%f", center.x, center.y, rightmost.x, rightmost.y);

            calloutEl.setAttributeNS(null, "d", calloutPath);

            final HeatmapStyle style = HeatmapStyle.forUpDnValues(organismPart.up, organismPart.dn);

            switch (style) {
                case UpDn:
                    editor.fill(rectId, "blue");
                    editor.fill(triangleId, "red");
                    editor.setTextAndAlign(textCalloutUpId, String.valueOf(organismPart.up));
                    editor.setTextAndAlign(textCalloutDnId, String.valueOf(organismPart.dn));
                    editor.setVisibility(textCalloutCenterId, "hidden");

                    editor.fill(organismPart.id, "grey");
                    editor.setOpacity(organismPart.id, "0.5");
                    break;
                case Up:
                    editor.fill(rectId, "red");
                    editor.setVisibility(triangleId, "hidden");
                    editor.setTextAndAlign(textCalloutCenterId, String.valueOf(organismPart.up));
                    editor.setVisibility(textCalloutUpId, "hidden");
                    editor.setVisibility(textCalloutDnId, "hidden");

                    editor.fill(organismPart.id, "red");
                    editor.setOpacity(organismPart.id, "0.5");
                    break;
                case Dn:
                    editor.fill(rectId, "blue");
                    editor.setVisibility(triangleId, "hidden");
                    editor.setTextAndAlign(textCalloutCenterId, String.valueOf(organismPart.dn));
                    editor.setVisibility(textCalloutUpId, "hidden");
                    editor.setVisibility(textCalloutDnId, "hidden");

                    editor.fill(organismPart.id, "blue");
                    editor.setOpacity(organismPart.id, "0.5");
                    break;
                case Blank:
                    editor.fill(rectId, "none");
                    editor.setVisibility(triangleId, "hidden");
                    editor.setText(textCalloutCenterId, String.valueOf(0));
                    editor.setVisibility(textCalloutUpId, "hidden");
                    editor.setVisibility(textCalloutDnId, "hidden");
                    editor.setStroke(textCalloutCenterId, "black");

                    editor.setOpacity(organismPart.id, "0.5");
                    break;
            }

            editor.setText(textCalloutCaptionId, organismPart.caption);
        }
    }

    private void leaveBest() {
        Collections.sort(organismParts);
        organismParts = organismParts.subList(0, Math.min(MAX_STRINGS_IN_LEGEND, organismParts.size()));
    }
}
