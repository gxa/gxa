package uk.ac.ebi.gxa.dao.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * as from http://stackoverflow.com/questions/2327423/how-to-validate-database-schema-programmatically-in-hibernate-with-annotations
 */
public class SchemaValidatingAnnotationSessionFactoryBean extends AnnotationSessionFactoryBean {
    public void validateDatabaseSchema() throws DataAccessException {
        logger.info("Validating database schema for Hibernate SessionFactory");
        HibernateTemplate hibernateTemplate = new HibernateTemplate(
                getSessionFactory());
        hibernateTemplate.setFlushMode(HibernateTemplate.FLUSH_NEVER);
        hibernateTemplate.execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session)
                    throws HibernateException, SQLException {
                Connection con = session.connection();
                Dialect dialect = Dialect.getDialect(getConfiguration().getProperties());
                DatabaseMetadata metadata = new DatabaseMetadata(con, dialect);
                Configuration configuration = getConfiguration();
                configuration.validateSchema(dialect, metadata);
                return null;
            }
        });
    }
}
