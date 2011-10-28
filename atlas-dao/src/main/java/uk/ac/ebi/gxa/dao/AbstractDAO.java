package uk.ac.ebi.gxa.dao;

import com.google.common.collect.Iterables;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;

import java.util.List;

public abstract class AbstractDAO<T> {
    protected final HibernateTemplate template;
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

    protected AbstractDAO(SessionFactory sessionFactory, Class<T> clazz) {
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
     * @throws uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException
     *
     */
    public T getByName(String name) throws RecordNotFoundException {
        return getByName(name, getNameColumn());
    }

    /**
     * @param name
     * @param colName
     * @return instance of class T that matches name, searching through colName
     * @throws uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException
     *
     */
    public T getByName(String name, String colName) throws RecordNotFoundException {
        @SuppressWarnings("unchecked")
        final List<T> results = template.find("from " + clazz.getSimpleName() + " where " + colName + " = ?", (lowerCaseNameMatch() ? name.toLowerCase() : name));
        return getFirst(results, name);
    }

    /**
     * @param objects
     * @param name
     * @return first element of objects
     * @throws uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException
     *          if objects' length == 0
     */
    protected T getFirst(List<T> objects, String name) throws RecordNotFoundException {
        if (objects.size() != 1)
            throw new RecordNotFoundException(clazz.getName() + ": " + name + " not found" +
                    " (" + objects.size() + "objects returned)");
        return Iterables.getFirst(objects, null);
    }
}
