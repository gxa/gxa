/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.web.wro4j.tag.config;

import org.apache.commons.digester.AbstractObjectCreationFactory;
import org.xml.sax.Attributes;

/**
 * @author Olga Melnichuk
 */
public class Wro4jGroupCreationFactory extends AbstractObjectCreationFactory {
    @Override
    public Object createObject(Attributes attributes) throws Exception {
        String name = attributes.getValue("name");
        if (name == null) {
            throw new IllegalArgumentException();
        }
        return new Wro4jGroup(name);
    }
}
