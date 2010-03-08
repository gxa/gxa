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
package uk.ac.ebi.gxa.properties;

/**
 * Atlas properties storage backend interface
 * @author pashky
 */
public interface Storage {
    /**
     * Sets (or deletes) property value by name
     * @param name property name
     * @param value new property value, or null if storage should delete property customization
     */
    void setProperty(String name, String value);

    /**
     * Retrievescurrent  property value
     * @param name property name
     * @return current property value or null if not found
     */
    String getProperty(String name);

    /**
     * Checks if this storage can be used to store values permanently
     * @return true if yes
     */
    boolean isWritePersistent();

    /**
     * Asks storage to reload its sources
     */
    void reload();
}
