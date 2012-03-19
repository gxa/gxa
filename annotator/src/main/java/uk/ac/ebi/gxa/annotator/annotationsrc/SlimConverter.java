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

package uk.ac.ebi.gxa.annotator.annotationsrc;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 19/03/2012
 */
public class SlimConverter {

    public Properties loadPropertiesFromText(String text) throws AnnotationLoaderException {
        Reader input = new StringReader(text);
        Properties properties = new Properties();
        try {
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new AnnotationLoaderException("Cannot read annotation properties", e);
        } finally {
            closeQuietly(input);
        }
    }
}
