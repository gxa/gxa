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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * BioMart v 7.0 registry
 *
 * A registry XML file dictates which datasets in which marts on which database servers are available for querying.
 * E.g. http://plants.ensembl.org/biomart/martservice?type=registry
 *
 * @author Olga Melnichuk
 */
class MartRegistry {

    private final List<MartUrlLocation> urlLocations = new ArrayList<MartUrlLocation>();

    private MartRegistry() {
    }

    public MartUrlLocation find(String database) {
        for (MartUrlLocation loc : urlLocations) {
            String db = loc.getDatabase();
            if (db.startsWith(database)) {
                return loc;
            }
        }
        return null;
    }

    private void add(MartUrlLocation location) {
        urlLocations.add(location);
    }

    public static MartRegistry parse(InputStream in) throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser = spf.newSAXParser();

        XmlContentHandler handler = new XmlContentHandler();
        parser.parse(in, handler);

        return handler.getRegistry();
    }

    public static class MartUrlLocation {
        private final String database;
        private final String name;
        private final String virtualSchema;

        private MartUrlLocation(String database, String name, String virtualSchema) {
            this.database = database;
            this.name = name;
            this.virtualSchema = virtualSchema;
        }

        public String getDatabase() {
            return database;
        }

        public String getName() {
            return name;
        }

        public String getVirtualSchema() {
            return virtualSchema;
        }
    }

    private static class XmlContentHandler extends DefaultHandler {
        private final MartRegistry registry = new MartRegistry();

        public MartRegistry getRegistry() {
            return registry;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equalsIgnoreCase("MartURLLocation")) {
                String database = getAttribute(attributes, "database");
                String name = getAttribute(attributes, "name");
                String virtualSchema = getAttribute(attributes, "serverVirtualSchema");
                registry.add(new MartUrlLocation(database, name, virtualSchema));
            }
        }

        private String getAttribute(Attributes attributes, String attrName) {
            int index = attributes.getIndex(attrName);
            return index >= 0 ? attributes.getValue(index) : null;
        }
    }
}
