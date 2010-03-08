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

/**
 * Oracle DB - backed storage. Can store value permanently.
 * @author pashky
 */
public class DbStorage implements Storage {
    private Logger log = LoggerFactory.getLogger(getClass());

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setProperty(String name, String value) {
        try {
            if(jdbcTemplate.update(
                    "MERGE INTO A2_CONFIG_PROPERTY t USING DUAL ON (t.name = ?) " +
                            "WHEN MATCHED THEN UPDATE SET value = ? " +
                            "WHEN NOT MATCHED THEN INSERT (name,value) values (?,?)",
                    new Object[] {
                            name,
                            value
                    }) != 1)
                throw new IncorrectResultSizeDataAccessException(1);
        } catch (DataAccessException e) {
            log.error("Can't store configuration property " + name + "=" + value, e);
        }
    }

    public String getProperty(String name) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT stage FROM A2_CONFIG_PROPERTY t WHERE ts.name = ?",
                    new Object[] { name }, String.class).toString();
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (DataAccessException e) {
            log.error("Can't retrieve configuration property " + name, e);
            return null;
        }
    }

    public boolean isWritePersistent() {
        return true;
    }

    public void reload() {
        // do nothing
    }
}
