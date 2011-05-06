package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;

abstract class AbstractDAO<T> {
    protected final JdbcTemplate template;

    public AbstractDAO(JdbcTemplate template) {
        this.template = template;
    }

    public abstract T getById(long id);

    protected abstract String sequence();

    protected abstract void save(T object);

    protected Long nextId() {
        return template.queryForLong("select " + sequence() + ".NEXT from DUAL");
    }
}
