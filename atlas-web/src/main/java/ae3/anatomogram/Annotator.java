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

package ae3.anatomogram;

import ae3.model.AtlasGene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import org.apache.batik.dom.util.DOMUtilities;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.parser.PathHandler;
import uk.ac.ebi.gxa.requesthandlers.genepage.AnatomogramRequestHandler;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.io.*;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.*;

public class Annotator {
    public class Area{
        public int X0,X1,Y0,Y1;
        public String Name;
        public String Efo;
        public String getEfo(){
            return Efo;
        }
        public String getX0(){
            return String.valueOf(X0);
        }
        public String getX1(){
            return String.valueOf(X1);
        }
        public String getY0(){
            return String.valueOf(Y0);
        }
        public String getY1(){
            return String.valueOf(Y1);
        }
    }

    public enum AnatomogramType{
         Das
        ,Web
    };

    public static final int MAX_ANNOTATIONS = 9;
    public static final String EFO_GROUP_ID ="LAYER_EFO";
    public static Map<AnatomogramType, Map<String,Document>> templatedocuments = new HashMap<AnatomogramType, Map<String,Document>>(); //organism->template
    public static Document emptyDocument;
    final private Logger log = LoggerFactory.getLogger(getClass());
    private List<Area> map = new ArrayList<Area>();


    private Document loadDocument(String filename) throws Exception{
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);

        InputStream stream = getClass().getResourceAsStream(filename); //Human_Male
        Document result = null;
        try {
             result = f.createDocument(/*uri*/ null, stream);
        }
        finally {
            if (null != stream) {
                stream.close();
            }
        }
        return result; 
    }

    public void load() {
        try {
                templatedocuments.put(AnatomogramType.Das,new HashMap<String,Document>());
                for(String[] organism : new String[][]{{"homo sapiens","/Human_Male.svg"}
                                                  ,{"mus musculus","/mouse.svg"}
                                                  ,{"drosophila melanogaster","/fly.svg"}
                                                  ,{"rattus norvegicus","/rat.svg"}}){

                    templatedocuments.get(AnatomogramType.Das).put(organism[0],loadDocument(organism[1]));
                }//organism cycle

                templatedocuments.put(AnatomogramType.Web,new HashMap<String,Document>());
                for(String[] organism : new String[][]{{"homo sapiens","/Human_web.svg"}
                                                      ,{"mus musculus","/mouse_web.svg"}
                                                      ,{"drosophila melanogaster","/fly_web.svg"}
                                                      ,{"rattus norvegicus","/rat_web.svg"}}){

                    templatedocuments.get(AnatomogramType.Web).put(organism[0],loadDocument(organism[1]));
                }//organism cycle
            emptyDocument = loadDocument("/empty.svg");
        }
        catch (Exception ex) {
            log.error("can not load anatomogram template", ex);
        }
    }

    public static void ParsePath(Document document, String ElementId, PathHandler pathHandler) {
        Element path = document.getElementById(ElementId);
        String s_efo0 = path.getAttribute("d");
        org.apache.batik.parser.PathParser pa = new org.apache.batik.parser.PathParser();
        pa.setPathHandler(pathHandler);
        pa.parse(s_efo0);
    }

    public static String xmlToString(Node node) {
        try {
            Source source = new DOMSource(node);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
            return stringWriter.getBuffer().toString();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public enum Encoding {
        Svg, Jpeg, Png
    }

    enum HeatmapStyle {
        UpDn, Up, Dn, Blank
    }

    public List<String> getKnownEfo(AnatomogramType anatomogramType, String organism){
        if(!templatedocuments.get(anatomogramType).containsKey(organism.toLowerCase())){
            return Collections.emptyList(); //do not fail if not found
        }
        List<String> result = new ArrayList<String>();
        Element layer = templatedocuments.get(anatomogramType).get(organism.toLowerCase()).getElementById(EFO_GROUP_ID);
        NodeList nl =  layer.getChildNodes();
        for(int i=0; i!=nl.getLength(); i++){
            Node n = nl.item(i);
            if(null==n)
                continue;
            org.w3c.dom.NamedNodeMap nnm = n.getAttributes();
            if(null==nnm)
                continue;
            Node n2 = nnm.getNamedItem("id");
            if(null==n2)
                continue;
            result.add(n2.getNodeValue());
        }
        return result;
    }

    /*figure out if something to show on the anatomogram. FALSE if
    1) gene=null
    2) unknown organism
    3) no expressions for known efo
    */
    public boolean getHasAnatomogram(AtlasGene gene,AnatomogramType anatomogramType){
        if(null==gene)
            return false;
        List<String> anatomogramEfoList = this.getKnownEfo(anatomogramType, gene.getGeneSpecies());
        if(null==anatomogramEfoList)
            return false;
        for(String term : anatomogramEfoList){
            if(gene.getCount_dn(term)>0||gene.getCount_up(term)>0)
                return true;
        }
        return false;
    }

    /* render pic to out stream, throw exception if not found
    */
    public void process(String organism, List<AnatomogramRequestHandler.Annotation> annotations, Encoding encoding, OutputStream stream, AnatomogramType anatomogramType) throws Exception {
        class Dot {
            float x;
            float y;
        }
        if(!templatedocuments.get(anatomogramType).containsKey(organism.toLowerCase())){
            throw new IllegalArgumentException(String.format("can not find anatomogram for %1$s",organism));
        }
        Document document = (Document) templatedocuments.get(anatomogramType).get(organism.toLowerCase()).cloneNode(true);
        final Map<String, Dot> EFOs = new HashMap<String, Dot>();
        ListIterator<AnatomogramRequestHandler.Annotation> i_a = annotations.listIterator();
        while (i_a.hasNext()) {
            AnatomogramRequestHandler.Annotation current_annotation = i_a.next();
            String pathId = current_annotation.id;
            AnnotationPathHandler annotationPathHandler = new AnnotationPathHandler();
            ParsePath(document, pathId, annotationPathHandler);
            Dot coord = new Dot();
            coord.x = annotationPathHandler.getCenterX();
            coord.y = annotationPathHandler.getCenterY();
            EFOs.put(pathId, coord);
            log.debug("EFO:" + pathId + " " + coord.x + " " + coord.y);
        }
        for (int i = 1; i <= MAX_ANNOTATIONS; i++) {
            Editor editor = new Editor(document);
            final String calloutId = String.format("pathCallout%1$d", i);
            final String rectId = String.format("rectCallout%1$d", i);
            final String triangleId = String.format("triangleCallout%1$d", i);
            final String textCalloutUpId = String.format("textCalloutUp%1$d", i);
            final String textCalloutDnId = String.format("textCalloutDn%1$d", i);
            final String textCalloutCenterId = String.format("textCalloutCenter%1$d", i);
            final String textCalloutCaptionId = String.format("textCalloutCaption%1$d", i);
            if (!annotations.listIterator().hasNext()) {
                editor.setVisibility(calloutId, "hidden");
                editor.setVisibility(rectId, "hidden");
                editor.setVisibility(triangleId, "hidden");
                editor.setVisibility(textCalloutUpId, "hidden");
                editor.setVisibility(textCalloutDnId, "hidden");
                editor.setVisibility(textCalloutCenterId, "hidden");
                editor.setVisibility(textCalloutCaptionId, "hidden");
                continue;
            }
            if (null == document.getElementById(calloutId))
                throw new Exception("can not find element" + calloutId);
            CalloutPathHandler calloutPathHandler = new CalloutPathHandler();
            ParsePath(document, calloutId, calloutPathHandler);
            final float X = calloutPathHandler.getRightmostX();
            final float Y = calloutPathHandler.getRightmostY();
            Collections.sort(annotations, new Comparator<AnatomogramRequestHandler.Annotation>() {
                private float metric(AnatomogramRequestHandler.Annotation a) {
                    return (Y - EFOs.get(a.id).y) / (X - EFOs.get(a.id).x);
                }
                public int compare(AnatomogramRequestHandler.Annotation a1, AnatomogramRequestHandler.Annotation a2) {
                    return Float.compare(metric(a2), metric(a1));
                }
            });
            final AnatomogramRequestHandler.Annotation current_annotation = annotations.listIterator().next();
            String calloutPath = String.format("M %f,%f L %f,%f"
                    , EFOs.get(current_annotation.id).x
                    , EFOs.get(current_annotation.id).y
                    , X
                    , Y);
            document.getElementById(calloutId).setAttributeNS(null, "d", calloutPath);
            final HeatmapStyle Style;
            if ((current_annotation.up > 0) && (current_annotation.dn > 0)) {
                Style = HeatmapStyle.UpDn;
            } else if (current_annotation.up > 0) {
                Style = HeatmapStyle.Up;
            } else if (current_annotation.dn > 0) {
                Style = HeatmapStyle.Dn;
            } else {
                Style = HeatmapStyle.Blank;
            }
            switch (Style) {
                case UpDn:
                    editor.fill(rectId, "blue");
                    editor.fill(triangleId, "red");
                    editor.setTextAndAlign(textCalloutUpId, String.format("%1$d", current_annotation.up));
                    editor.setTextAndAlign(textCalloutDnId, String.format("%1$d", current_annotation.dn));
                    editor.setVisibility(textCalloutCenterId, "hidden");
                    editor.fill(current_annotation.id, "grey");
                    editor.setOpacity(current_annotation.id, "0.5");
                    break;
                case Up:
                    editor.fill(rectId, "red");
                    editor.setVisibility(triangleId, "hidden");
                    editor.setTextAndAlign(textCalloutCenterId, String.format("%1$d", current_annotation.up));
                    editor.setVisibility(textCalloutUpId, "hidden");
                    editor.setVisibility(textCalloutDnId, "hidden");
                    editor.fill(current_annotation.id, "red");
                    editor.setOpacity(current_annotation.id, "0.5");
                    break;
                case Dn:
                    editor.fill(rectId, "blue");
                    editor.setVisibility(triangleId, "hidden");
                    editor.setTextAndAlign(textCalloutCenterId, String.format("%1$d", current_annotation.dn));
                    editor.setVisibility(textCalloutUpId, "hidden");
                    editor.setVisibility(textCalloutDnId, "hidden");
                    editor.fill(current_annotation.id, "blue");
                    editor.setOpacity(current_annotation.id, "0.5");
                    break;
                case Blank:
                    editor.fill(rectId, "none");
                    editor.setVisibility(triangleId, "hidden");
                    editor.setText(textCalloutCenterId, String.format("%1$d", 0));
                    editor.setVisibility(textCalloutUpId, "hidden");
                    editor.setVisibility(textCalloutDnId, "hidden");
                    editor.setStroke(textCalloutCenterId, "black");
                    editor.setOpacity(current_annotation.id, "0.5");
                    break;
            }
            editor.setText(textCalloutCaptionId, current_annotation.caption);
            {
                Float x = Float.parseFloat(document.getElementById(rectId).getAttribute("x"));
                Float y = Float.parseFloat(document.getElementById(rectId).getAttribute("y"));
                Float height = Float.parseFloat(document.getElementById(rectId).getAttribute("height"));
                Float width = Float.parseFloat(document.getElementById(rectId).getAttribute("width"));

                Area area = new Area();
                area.X0 = x.intValue();
                area.X1 = ((Float)(x + width)).intValue() + 200;
                area.Y0 = y.intValue();
                area.Y1 = ((Float)(y + height)).intValue();
                area.Name = current_annotation.caption;
                area.Efo = current_annotation.id;

                map.add(area);
            }
            annotations.remove(current_annotation);
        }
        if(stream!=null){
            WriteDocumentToStream(document,encoding,stream);
        }
    }

    public void WriteDocumentToStream(Document document, Encoding encoding, OutputStream stream) throws Exception{
        switch (encoding) {
            case Svg: {
                DOMUtilities.writeDocument(document, new OutputStreamWriter(stream, "UTF-8"));
                break;
            }
            case Jpeg: {
                JPEGTranscoder t = new JPEGTranscoder();
	            // t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(. 8));
                TranscoderInput input = new TranscoderInput(document);
                TranscoderOutput output = new TranscoderOutput(stream);
                t.transcode(input, output);
                break;
            }
            case Png: {
                PNGTranscoder t = new PNGTranscoder();
                //t.addTranscodingHint(JPEGTranscoder.KEY_WIDTH, new Float(350));
                //t.addTranscodingHint(JPEGTranscoder.KEY_HEIGHT, new Float(150));
                TranscoderInput input = new TranscoderInput(document);
                TranscoderOutput output = new TranscoderOutput(stream);
                t.transcode(input, output);
                break;
            }
            default:
                throw new InvalidParameterException("unknown encoding");
        }
    }

    public void getEmptyPicture(Encoding encoding, OutputStream stream) throws Exception{
        Document document = (Document) emptyDocument;
        WriteDocumentToStream(document,encoding,stream);
    }

    public List<Area> getMap(){
        return this.map;
    }
}