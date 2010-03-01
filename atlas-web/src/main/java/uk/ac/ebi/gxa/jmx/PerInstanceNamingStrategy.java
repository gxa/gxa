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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.jmx;

import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import java.util.Hashtable;

/**
 * @author pashky
 */
public class PerInstanceNamingStrategy implements ObjectNamingStrategy, ApplicationContextAware {
    private String domain;
    private String contextId;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // fixme: any ideas on what to use as unqiue instance id?
        contextId = Integer.toHexString(applicationContext.hashCode()) + Integer.toHexString(this.hashCode());
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public ObjectName getObjectName(Object o, String s) throws MalformedObjectNameException {
        Hashtable<String,String> table = new Hashtable<String, String>();
        table.put("name", s);
        table.put("instance", contextId);
        return ObjectName.getInstance(domain, table);
    }
}
