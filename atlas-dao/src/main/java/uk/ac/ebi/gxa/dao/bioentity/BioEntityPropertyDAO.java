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

package uk.ac.ebi.gxa.dao.bioentity;

import org.hibernate.SessionFactory;
import uk.ac.ebi.gxa.dao.AbstractDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;

/**
 * User: nsklyar
 * Date: 24/05/2011
 */
public class BioEntityPropertyDAO extends AbstractDAO<BioEntityProperty> {

    BioEntityPropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory, BioEntityProperty.class);
    }

    @Override
    protected String getNameColumn() {
        return "name";
    }

    public BioEntityProperty findOrCreate(String propertyName) {
        try {
            return getByName(propertyName);
        } catch (RecordNotFoundException e) {
            BioEntityProperty property = new BioEntityProperty(null, propertyName);
            save(property);
            template.flush();
            return property;
        }
    }

}
