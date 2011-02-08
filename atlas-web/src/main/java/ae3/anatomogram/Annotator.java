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
import ae3.service.AtlasStatisticsQueryService;
import com.google.common.io.Closeables;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.statistics.StatisticsQueryUtils;
import uk.ac.ebi.gxa.statistics.StatisticsType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Annotator {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private AtlasStatisticsQueryService atlasStatisticsQueryService;

    public void setAtlasStatisticsQueryService(AtlasStatisticsQueryService atlasStatisticsQueryService) {
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
    }

    public enum AnatomogramType {
        Das, Web
    }

    private Map<AnatomogramType, Map<String, Document>> templateDocuments = new HashMap<AnatomogramType, Map<String, Document>>(); //organism->template
    private Anatomogram emptyAnatomogram;
    private Efo efo;

    public void setEfo(Efo efo) {
        this.efo = efo;
    }

    private Document loadDocument(String filename) throws IOException {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);

        InputStream stream = null;
        Document result = null;
        try {
            stream = getClass().getResourceAsStream(filename);
            result = f.createDocument(/*uri*/ null, stream);
        } finally {
            Closeables.closeQuietly(stream);
        }
        return result;
    }

    public void load() {
        try {
            templateDocuments.put(AnatomogramType.Das, new HashMap<String, Document>());
            for (String[] organism : new String[][]{{"homo sapiens", "/Human_Male.svg"}
                    , {"mus musculus", "/mouse.svg"}
                    , {"drosophila melanogaster", "/fly.svg"}
                    , {"rattus norvegicus", "/rat.svg"}}) {

                templateDocuments.get(AnatomogramType.Das).put(organism[0], loadDocument(organism[1]));
            }//organism cycle

            templateDocuments.put(AnatomogramType.Web, new HashMap<String, Document>());
            for (String[] organism : new String[][]{{"homo sapiens", "/Human_web.svg"}
                    , {"mus musculus", "/mouse_web.svg"}
                    , {"drosophila melanogaster", "/fly_web.svg"}
                    , {"rattus norvegicus", "/rat_web.svg"}}) {

                templateDocuments.get(AnatomogramType.Web).put(organism[0], loadDocument(organism[1]));
            }//organism cycle
            emptyAnatomogram = createAnatomogram(loadDocument("/empty.svg"));
        } catch (IOException ex) {
            log.error("can not load anatomogram template", ex);
        }
    }

    private Document findDocument(AnatomogramType anatomogramType, String organism) {
        Document doc = null;

        Map<String, Document> docByType = templateDocuments.get(anatomogramType);
        if (docByType != null) {
            doc = docByType.get(organism.toLowerCase());
        }

        return doc;
    }

    public Anatomogram getAnatomogram(AnatomogramType anatomogramType, AtlasGene gene) {
        Document doc = findDocument(anatomogramType, gene.getGeneSpecies());
        Anatomogram an = null;

        long bitIndexAccessTime = 0;
        if (doc != null) {
            for (String acc : getKnownEfo(doc)) {
                EfoTerm term = efo.getTermById(acc);

                boolean isEfo = StatisticsQueryUtils.EFO;

                long start = System.currentTimeMillis();
                int dn = atlasStatisticsQueryService.getExperimentCountsForGene(acc, StatisticsType.DOWN, isEfo, gene.getGeneId());
                int up = atlasStatisticsQueryService.getExperimentCountsForGene(acc, StatisticsType.UP, isEfo, gene.getGeneId());
                bitIndexAccessTime += System.currentTimeMillis() - start;

                if ((dn > 0) || (up > 0)) {
                    if (an == null) {
                        an = createAnatomogram(doc);
                    }
                    an.addAnnotation(acc, term.getTerm(), up, dn);
                }
            }
        }
        log.info("Retrieved stats from bit index for " + gene.getGeneName() + "'s anatomogram in: " + bitIndexAccessTime + " ms");


        return an == null ? emptyAnatomogram : an;
    }

    public Anatomogram getEmptyAnatomogram() {
        return emptyAnatomogram;
    }

    private Anatomogram createAnatomogram(Document doc) {
        return new Anatomogram((Document) doc.cloneNode(true));
    }

    private List<String> getKnownEfo(Document doc) {
        List<String> result = new ArrayList<String>();

        Element layer = doc.getElementById("LAYER_EFO");
        if (layer == null) {
            log.warn("No LAYER_EFO found");
            return result;
        }

        NodeList nl = layer.getChildNodes();
        for (int i = 0; i != nl.getLength(); i++) {
            Node n = nl.item(i);
            if (null == n)
                continue;
            org.w3c.dom.NamedNodeMap nnm = n.getAttributes();
            if (null == nnm)
                continue;
            Node n2 = nnm.getNamedItem("id");
            if (null == n2)
                continue;
            result.add(n2.getNodeValue());
        }
        return result;
    }
}