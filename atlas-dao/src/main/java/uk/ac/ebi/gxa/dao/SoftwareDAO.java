package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.wiring.BeanWiringInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.bioentity.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SoftwareDAO extends AbstractDAO<Software> {


    SoftwareDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Software.class);
    }


    public Software find(String name, String newVersion) {
        Software software = new Software(name, newVersion);
        save(software);
        return software;
    }

//    @Override
//    public void save(Software object) {
//        super.save(object);
//        template.flush();
//    }
}