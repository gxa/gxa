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

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author pashky
 */
public class RServicePropertyStorage implements Storage {
    private DataSource dataSource;
    private ResourceFileStorage restOfProps = new ResourceFileStorage();
    private Map<String,String> computedProperties;

    private final static String PREFIX = "atlas.rservice.";

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setResourcePath(String resourcePath) {
        this.restOfProps.setResourcePath(resourcePath);
    }

    public void setProperty(String name, String value) {
    }

    public String getProperty(String name) {
        initDataSourceProperties();
        String value = restOfProps.getProperty(name);
        if(value != null)
            return value;
        return computedProperties.get(name);
    }

    private void initDataSourceProperties() {
        if(computedProperties == null) {
            computedProperties = new HashMap<String, String>();
            computedProperties.put(PREFIX + "biocep.db.url", callFuzzyMethod(dataSource, "url"));
            computedProperties.put(PREFIX + "biocep.db.user", callFuzzyMethod(dataSource, "user"));
            computedProperties.put(PREFIX + "biocep.db.password", callFuzzyMethod(dataSource, "password"));

            String drvclass = callFuzzyMethod(dataSource, "driverclass");
            computedProperties.put(PREFIX + "biocep.db.driver", drvclass);
            if(drvclass.toLowerCase().contains("oracle"))
                computedProperties.put(PREFIX + "biocep.db.type", "oracle");
            // map some system variables
            for(String s : new String[] { "R_HOME", "R.remote.host" })
                computedProperties.put(PREFIX + s, System.getProperty(s));
        }
    }

    private String callFuzzyMethod(Object o, String method) {
        Method[] methods = o.getClass().getMethods();
        for(Method m : methods)
            if(m.getParameterTypes().length == 0
                    && m.getName().startsWith("get")
                    && m.getName().toLowerCase().contains(method))
                try {
                    return m.invoke(o).toString();
                } catch(Exception e) {
                    return null;
                }
        return null;
    }

    public boolean isWritePersistent() {
        return false;
    }

    public Collection<String> getAvailablePropertyNames() {
        initDataSourceProperties();
        List<String> all = new ArrayList<String>(computedProperties.keySet());
        all.addAll(restOfProps.getAvailablePropertyNames());
        return all;
    }

    public void reload() {
        initDataSourceProperties();
        restOfProps.reload();
    }
}
