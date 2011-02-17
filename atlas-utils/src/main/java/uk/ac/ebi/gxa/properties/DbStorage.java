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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Oracle DB - backed storage. Can store value permanently.
 *
 * @author pashky
 */
public class DbStorage implements Storage {
    private static final long REFRESH_PERIOD = 1000L * 60;
    private Logger log = LoggerFactory.getLogger(getClass());

    private Map<String, String> properties = new TreeMap<String, String>();
    private long timestamp = -1;

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setProperty(String name, String value) {
        timestamp = -1;

        if (value == null) {
            log.info("Deleting customization for property " + name);
            jdbcTemplate.update("DELETE FROM A2_CONFIG_PROPERTY t WHERE t.name = ?", name);
            return;
        }

        log.info("Setting property " + name + " to new value " + value);
        try {
            if (jdbcTemplate.update(
                    "MERGE INTO A2_CONFIG_PROPERTY t USING DUAL ON (t.name = ?) " +
                            "WHEN MATCHED THEN UPDATE SET value = ? " +
                            "WHEN NOT MATCHED THEN INSERT (name,value) values (?,?)",
                    name, value, name, value
            ) != 1)
                throw new IncorrectResultSizeDataAccessException(1);
        } catch (DataAccessException e) {
            log.error("Can't store configuration property " + name + "=" + value, e);
        }
    }

    public synchronized String getProperty(String name) {
        readProperties();
        return properties.get(name);
    }

    private synchronized void readProperties() {
        if (System.currentTimeMillis() - timestamp < REFRESH_PERIOD)
            return;

        try {
            properties = new TreeMap<String, String>();
            jdbcTemplate.query(
                    "SELECT name, value FROM A2_CONFIG_PROPERTY t",
                    new RowCallbackHandler() {
                        public void processRow(ResultSet rs) throws SQLException {
                            properties.put(rs.getString("name"), rs.getString("value"));
                        }
                    });
            timestamp = System.currentTimeMillis();
        } catch (EmptyResultDataAccessException e) {
            log.error("Can't retrieve DB configuration", e);
        } catch (DataAccessException e) {
            log.error("Can't retrieve DB configuration", e);
        }
    }

    public boolean isWritePersistent() {
        return true;
    }

    public synchronized Collection<String> getAvailablePropertyNames() {
        readProperties();
        return properties.keySet();
    }

    public void reload() {
        readProperties();
    }
}
