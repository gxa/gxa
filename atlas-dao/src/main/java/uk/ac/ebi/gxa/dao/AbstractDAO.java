package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.util.List;

abstract class AbstractDAO<T> {
    final HibernateTemplate template;
    private final Class<T> clazz;

    AbstractDAO(SessionFactory sessionFactory, Class<T> clazz) {
        this.clazz = clazz;
        this.template = new HibernateTemplate(sessionFactory);
    }

    public T getById(long id) {
        return clazz.cast(template.get(clazz, id));
    }

    @SuppressWarnings("unchecked")
    public List<T> getAll() {
        return template.find("from " + clazz.getName());
    }

    public void save(T object) {
        template.saveOrUpdate(object);
    }
}
