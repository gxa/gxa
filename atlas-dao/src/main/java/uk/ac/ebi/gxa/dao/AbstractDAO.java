package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;

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

    public void save(T object) {
        template.saveOrUpdate(object);
    }
}
