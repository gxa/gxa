package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;

import java.util.List;

public abstract class AbstractDAO<T> {
    protected HibernateTemplate template;
    private final Class<T> clazz;

    /**
     * @return the name of column used as name for {@link #getByName} method
     */
    protected abstract String getNameColumn();

    protected FindPropertiesQueryBuilder findPropertiesQueryBuilder;

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

    @Autowired
    public void setFindPropertiesQueryBuilder(FindPropertiesQueryBuilder findPropertiesQueryBuilder){
        this.findPropertiesQueryBuilder = findPropertiesQueryBuilder ;
    }

    public FindPropertiesQueryBuilder getFindPropertiesQueryBuilder(){
        return findPropertiesQueryBuilder ;
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
        return getOnly(results);
    }

    /**
     * @param objects
     * @return first element of objects
     * @throws uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException
     *          if objects' length == 0
     */
    protected T getOnly(List<T> objects) throws RecordNotFoundException {
        if (objects.size() == 1)
            return objects.get(0);
        else
            throw new RecordNotFoundException(clazz.getName() + ": " + objects.size() + " objects returned; expected 1)");
    }


}
