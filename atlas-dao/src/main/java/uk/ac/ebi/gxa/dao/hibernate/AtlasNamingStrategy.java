package uk.ac.ebi.gxa.dao.hibernate;

import org.hibernate.cfg.DefaultNamingStrategy;

/**
 * A NamingStrategy reflecting Atlas DB prefixes, sequences, etc.
 */

public class AtlasNamingStrategy extends DefaultNamingStrategy {
    @Override
    public String classToTableName(String className) {
        return "A2_" + super.classToTableName(className).toUpperCase();
    }

    @Override
    public String foreignKeyColumnName(String propertyName, String propertyEntityName, String propertyTableName, String referencedColumnName) {
        return super.foreignKeyColumnName(propertyName, propertyEntityName, propertyTableName, referencedColumnName) + "ID";
    }
}
