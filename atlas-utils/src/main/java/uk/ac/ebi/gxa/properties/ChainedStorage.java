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

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

/**
 * A meta-storage containing a list of other storages serving values using first-matching basis
 * @author pashky
 */
public class ChainedStorage implements Storage {

    private Collection<Storage> storages;

    /**
     * Sets storage list
     * @param storages collection of storages to use
     */
    public void setStorages(Collection<Storage> storages) {
        this.storages = storages;
    }

    public void setProperty(String name, String value) {
        for(Storage storage : storages)
            if(storage.isWritePersistent()) {
                storage.setProperty(name, value);
                return;
            }
        if(!storages.isEmpty())
            storages.iterator().next().setProperty(name, value);
    }

    public String getProperty(String name) {
        for(Storage storage : storages) {
            String value = storage.getProperty(name);
            if(value != null)
                return value;
        }
        return null;
    }

    public boolean isWritePersistent() {
        for(Storage storage : storages)
            if(storage.isWritePersistent())
                return true;
        return false;
    }

    public Collection<String> getAvailablePropertyNames() {
        Set<String> result = new HashSet<String>();
        for(Storage storage : storages)
            result.addAll(storage.getAvailablePropertyNames());
        return result;
    }

    public void reload() {
        for(Storage storage : storages)
            storage.reload();
    }
}
