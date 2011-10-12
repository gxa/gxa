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
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
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

    public AnnotationSource getById(long id) {
        return template.get(AnnotationSource.class, id);
    }

    public void save(AnnotationSource object) {
        object.setLoadDate(new Date());
        template.save(object);
        template.flush();
    }

    @SuppressWarnings("unchecked")
    public <T extends AnnotationSource> Collection<T> getAnnotationSourcesOfType(Class<T> type) {
        return template.find("from " + type.getSimpleName());
    }

    public <T extends AnnotationSource> T findAnnotationSource(Software software, Organism organism, Class<T> type) {
        String queryString = "from " + type.getSimpleName() + " where software = ? and organism = ?";
        @SuppressWarnings("unchecked")
        final List<T> results = template.find(queryString, software, organism);
        return results.isEmpty() ? null : results.get(0);
    }

    public void remove(BioMartAnnotationSource annSrc) {

        template.delete(annSrc);
        template.flush();
    }

    public boolean isAnnSrcApplied(final AnnotationSource annSrc) {
        return isAnnSrcApplied(annSrc, false);
    }

    /**
     * @param annSrc
     * @param hsql   flag to indicate if the query is run as hsql (true) or an Oracle query (false). The flag is needed because
     *               hsql does not recognise ROWNUM (and we need hsql to junit test thsi method)
     * @return true if annotation source annSrc has been applied for bioentity properties
     */
    public boolean isAnnSrcApplied(final AnnotationSource annSrc, boolean hsql) {
        String query = "SELECT 1 FROM A2_BIOENTITYBEPV\n" +
                "WHERE SOFTWAREID = ?\n" +
                "AND BIOENTITYID = (SELECT BEPV.BIOENTITYID FROM A2_BIOENTITYBEPV BEPV JOIN A2_BIOENTITY BE ON BEPV.BIOENTITYID = BE.BIOENTITYID\n" +
                "WHERE BE.ORGANISMID=? " + (hsql ? "LIMIT 1" : "AND ROWNUM=1") + ")";
        List list = atlasJdbcTemplate.queryForList(query, annSrc.getSoftware().getSoftwareid(), annSrc.getOrganism().getId());

        return list.size() > 0;
    }

    public boolean isAnnSrcAppliedForArrayDesignMapping(final AnnotationSource annSrc, final ArrayDesign arrayDesign) {
        return isAnnSrcAppliedForArrayDesignMapping(annSrc, arrayDesign, false);
    }

    /**
     * @param annSrc
     * @param arrayDesign
     * @param hsql        hsql flag to indicate if the query is run as hsql (true) or an Oracle query (false). The flag is needed because
     *                    hsql does not recognise ROWNUM (and we need hsql to junit test this method)
     * @return true if annotation source annSrc has been applied for array design mappings
     */
    public boolean isAnnSrcAppliedForArrayDesignMapping(final AnnotationSource annSrc, final ArrayDesign arrayDesign, boolean hsql) {
        String query = "SELECT 1 FROM A2_DESIGNELTBIOENTITY\n" +
                "WHERE SOFTWAREID = ?\n" +
                "AND DESIGNELEMENTID IN (SELECT DE.DESIGNELEMENTID FROM A2_DESIGNELEMENT DE WHERE DE.ARRAYDESIGNID=?)\n" +
                (hsql ? " LIMIT 1" : " AND ROWNUM=1");
        List list = atlasJdbcTemplate.queryForList(query, annSrc.getSoftware().getSoftwareid(), arrayDesign.getArrayDesignID());

        return list.size() > 0;
    }
}
