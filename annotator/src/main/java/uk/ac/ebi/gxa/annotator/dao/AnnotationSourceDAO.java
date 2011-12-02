/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.annotator.dao;

import org.hibernate.SessionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate3.HibernateTemplate;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
public class AnnotationSourceDAO {

    private JdbcTemplate atlasJdbcTemplate;

    private final HibernateTemplate template;

    public AnnotationSourceDAO(SessionFactory sessionFactory, JdbcTemplate atlasJdbcTemplate) {
        this.template = new HibernateTemplate(sessionFactory);

        this.atlasJdbcTemplate = atlasJdbcTemplate;
    }

    public <T extends AnnotationSource> T getById(long id, Class<T> type) {
        return template.get(type, id);
    }

    public void save(AnnotationSource annSrc) {
        annSrc.setLoadDate(new Date());
        //ToDo: Find better solution.This method call is needed to initialize "name" field, if it was not called before name=null.
        // NS didn't manage to find how to force hibernate to access the field via getter.
        annSrc.getName();
        template.save(annSrc);
        template.flush();
    }

    public void update(AnnotationSource object) {
        template.update(object);
        template.flush();
    }

    @SuppressWarnings("unchecked")
    public <T extends AnnotationSource> Collection<T> getAnnotationSourcesOfType(Class<T> type) {
        return template.find("from " + type.getSimpleName());
    }

    public <T extends AnnotationSource> boolean isAnnSrcExistForSoftwareAndOrganism(Software software, Organism organism, Class<T> type) {
        String queryString = "from " + type.getSimpleName() + " where software = ? and organism = ?";
        @SuppressWarnings("unchecked")
        final List<T> results = template.find(queryString, software, organism);
        return !results.isEmpty();
    }

    public <T extends AnnotationSource> T findAnnotationSource(Software software, Organism organism, Class<T> type) {
        String queryString = "from " + type.getSimpleName() + " where software = ? and organism = ?";
        @SuppressWarnings("unchecked")
        final List<T> results = template.find(queryString, software, organism);
        return results.isEmpty() ? null : results.get(0);
    }

    public <T extends AnnotationSource> T findAnnotationSource(Software software, Class<T> type) {
        String queryString = "from " + type.getSimpleName() + " where software = ?";
        @SuppressWarnings("unchecked")
        final List<T> results = template.find(queryString, software);
        return results.isEmpty() ? null : results.get(0);
    }

    public <T extends AnnotationSource> boolean isAnnSrcExistForSoftware(Software software, Class<T> type) {
        String queryString = "from " + type.getSimpleName() + " where software = ?";
        @SuppressWarnings("unchecked")
        final List<T> results = template.find(queryString, software);
        return !results.isEmpty();
    }


    public void remove(AnnotationSource annSrc) {

        template.delete(annSrc);
        template.flush();
    }

    public boolean isAnnSrcAppliedForArrayDesignMapping(final AnnotationSource annSrc, final ArrayDesign arrayDesign) {
        String query = "SELECT count (DESIGNELEMENTID) FROM A2_DESIGNELTBIOENTITY\n" +
                "WHERE SOFTWAREID = ?\n" +
                "AND DESIGNELEMENTID IN (SELECT DE.DESIGNELEMENTID FROM A2_DESIGNELEMENT DE WHERE DE.ARRAYDESIGNID=?)";
        int count = atlasJdbcTemplate.queryForInt(query, annSrc.getSoftware().getSoftwareid(), arrayDesign.getArrayDesignID());

        return count > 0;
    }
}
