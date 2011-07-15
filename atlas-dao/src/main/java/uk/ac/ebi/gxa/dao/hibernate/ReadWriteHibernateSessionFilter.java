package uk.ac.ebi.gxa.dao.hibernate;

/**
 * This class overrides the default (read-only) OpenSessionInViewFilter to allow writing to DB
 * via hibernate (c.f. web.xml)
 *
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: 7/13/11
 * Time: 10:12 AM
 * To change this template use File | Settings | File Templates.
 */
import org.springframework.orm.hibernate3.support.OpenSessionInViewFilter;
import org.hibernate.*;
import org.springframework.dao.*;

public class ReadWriteHibernateSessionFilter extends OpenSessionInViewFilter {

    protected Session getSession(SessionFactory sessionFactory)
                        throws DataAccessResourceFailureException {
        Session session = super.getSession(sessionFactory);
        session.setFlushMode(FlushMode.COMMIT);
        return session;
    }

    protected void closeSession(Session session, SessionFactory factory) {
        session.flush();
        super.closeSession(session, factory);
    }
}