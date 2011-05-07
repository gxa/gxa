package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract class AbstractDAO<T> {
    protected final JdbcTemplate template;
    private final Map<Long, T> cache = new ConcurrentHashMap<Long, T>();

    public AbstractDAO(JdbcTemplate template) {
        this.template = template;
    }

    public T getById(long id) {
        T result = cache.get(id);
        return result != null ? result : loadById(id);
    }

    void registerObject(long id, T object) {
        cache.put(id, object);
    }

    protected abstract T loadById(long id);

    protected abstract String sequence();

    protected abstract void save(T object);

    protected Long nextId() {
        return template.queryForLong("select " + sequence() + ".NEXT from DUAL");
    }
}
