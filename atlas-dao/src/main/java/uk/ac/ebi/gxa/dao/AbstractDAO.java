package uk.ac.ebi.gxa.dao;

import com.google.common.collect.Iterables;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import uk.ac.ebi.gxa.dao.hibernate.DAOException;

import java.util.List;

abstract class AbstractDAO<T> {
    final HibernateTemplate template;
    private final Class<T> clazz;

    /**
     * @return the name of column used as name for {@link #getByName} method
     */
    protected abstract String getNameColumn();

    /**
     * @return false by default - case should match as is by default in getByName() queries
     */
    protected boolean lowerCaseNameMatch() {
        return false;
    }

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

    /**
     * @param name
     * @return instance of class T that matches name
     * @throws DAOException
     */
    public T getByName(String name) throws DAOException {
        return getByName(name, getNameColumn());
    }

    /**
     * @param name
     * @param colName
     * @return instance of class T that matches name, searching through colName
     * @throws DAOException
     */
    public T getByName(String name, String colName) throws DAOException {
        @SuppressWarnings("unchecked")
        final List<T> results = template.find("from " + clazz.getSimpleName() + " where " + colName + " = ?", (lowerCaseNameMatch() ? name.toLowerCase() : name));
        return getFirst(results, name);
    }

    /**
     * @param objects
     * @param name
     * @return first element of objects
     * @throws DAOException if objects' length == 0
     */
    protected T getFirst(List<T> objects, String name) throws DAOException {
        if (objects.isEmpty())
            throw new DAOException(clazz.getName() + ": " + name + " not found");
        return Iterables.getFirst(objects, null);
    }
}
