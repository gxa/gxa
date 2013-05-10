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
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;


/**
 * User: nsklyar
 * Date: 07/07/2011
 */
public class BioEntityTypeDAO extends AbstractDAO<BioEntityType> {

    private BioEntityPropertyDAO propertyDAO;

    BioEntityTypeDAO(SessionFactory sessionFactory, BioEntityPropertyDAO propertyDAO) {
        super(sessionFactory, BioEntityType.class);
        this.propertyDAO = propertyDAO;
    }

    public BioEntityType findOrCreate(String typeName) {
        try {
            return getByName(typeName);
        } catch (RecordNotFoundException e) {
            BioEntityProperty beProperty = propertyDAO.findOrCreate(typeName);
            BioEntityType type = new BioEntityType(null, typeName, 0, beProperty, beProperty);
            save(type);
            template.flush();
            return type;
        }
    }

   public void setUseForIndexEnsprotein(boolean useForIndex) {
       BioEntityType ensprotein = findOrCreate("ensprotein");
       ensprotein.setUseForIndex(useForIndex?1:0);
       save(ensprotein);
       template.flush();
   }

    @Override
    protected String getNameColumn() {
        return "name";
    }
}
