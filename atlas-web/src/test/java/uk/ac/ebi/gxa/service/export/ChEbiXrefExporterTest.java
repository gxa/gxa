/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.service.export;

import org.custommonkey.xmlunit.Diff;
import org.easymock.EasyMock;
import org.junit.Test;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.export.ChEbiEntry;

import java.util.ArrayList;
import java.util.Collection;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertTrue;

/**
 * User: nsklyar
 * Date: 02/05/2012
 */
public class ChEbiXrefExporterTest {
    @Test
    public void testExportChEbiEntries() throws Exception {
        ChEbiXrefExporter service = new ChEbiXrefExporter();
        service.setAtlasDAO(getAtlasDAO());
        final String xml = service.generateDataAsString();

        Diff myDiff = new Diff(EXPECTED_OUT, xml);
        assertTrue(myDiff.similar());

        assertXMLEqual(EXPECTED_OUT, xml);
    }

    private AtlasDAO getAtlasDAO() {
        final AtlasDAO atlasDAO = EasyMock.createMock(AtlasDAO.class);
        Collection<ChEbiEntry> mockCollection = new ArrayList<ChEbiEntry>();
        final ChEbiEntry chEbiEntry = new ChEbiEntry("CHEBI:1", "EXP1", "NAME1");
        chEbiEntry.addExperimentInfo("EXP1-1", "NAME1_1");
        mockCollection.add(chEbiEntry);
        mockCollection.add(new ChEbiEntry("CHEBI:2", "EXP2", "NAME2"));
        EasyMock.expect(atlasDAO.getChEbiEntries()).andReturn(mockCollection);
        EasyMock.replay(atlasDAO);
        return atlasDAO;
    }

    private static String EXPECTED_OUT = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<doc>\n" +
            "    <database_name>Gene Expression Atlas Database</database_name>\n" +
            "    <database_description>The Gene Expression Atlas is a semantically enriched database of meta-analysis based summary statistics over a curated subset of ArrayExpress Archive, servicing queries for condition-specific gene expression patterns as well as broader exploratory searches for biologically interesting genes/samples.</database_description>\n" +
            "    <link_url>http://www.ebi.ac.uk/gxa/experiment/*</link_url>\n" +
            "    <entities>\n" +
            "        <entity>\n" +
            "            <chebi_id>CHEBI:1</chebi_id>\n" +
            "            <xrefs>\n" +
            "                <xref>\n" +
            "                    <display_id>EXP1</display_id>\n" +
            "                    <link_id>EXP1</link_id>\n" +
            "                    <name>NAME1</name>\n" +
            "                </xref>\n" +
            "                <xref>\n" +
            "                    <display_id>EXP1-1</display_id>\n" +
            "                    <link_id>EXP1-1</link_id>\n" +
            "                    <name>NAME1_1</name>\n" +
            "                </xref>\n" +
            "            </xrefs>\n" +
            "        </entity>\n" +
            "        <entity>\n" +
            "            <chebi_id>CHEBI:2</chebi_id>\n" +
            "            <xrefs>\n" +
            "                <xref>\n" +
            "                    <display_id>EXP2</display_id>\n" +
            "                    <link_id>EXP2</link_id>\n" +
            "                    <name>NAME2</name>\n" +
            "                </xref>\n" +
            "            </xrefs>\n" +
            "        </entity>\n" +
            "    </entities>\n" +
            "</doc>";
}
