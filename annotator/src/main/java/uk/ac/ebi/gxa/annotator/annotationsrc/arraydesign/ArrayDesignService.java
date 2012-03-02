/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator.annotationsrc.arraydesign;

import org.springframework.stereotype.Service;
import uk.ac.ebi.gxa.dao.ArrayDesignDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

/**
 * User: nsklyar
 * Date: 02/09/2011
 */
@Service
public class ArrayDesignService {

    private ArrayDesignDAO arrayDesignDAO;

    public ArrayDesignService(ArrayDesignDAO arrayDesignDAO) {
        this.arrayDesignDAO = arrayDesignDAO;
    }

    public ArrayDesign findOrCreateArrayDesignShallow(String accession) {
        ArrayDesign arrayDesign = arrayDesignDAO.getArrayDesignShallowByAccession(accession, true);
        if (arrayDesign == null) {
            ArrayExpressConnection aeConnection = new ArrayExpressConnection(accession);
            arrayDesign = createNew(accession,
                    aeConnection.getName(),
                    aeConnection.getProvider(),
                    aeConnection.getType());

            arrayDesignDAO.save(arrayDesign);
        }

        return arrayDesign;
    }

    private ArrayDesign createNew(String accession, String name, String provider, String type) {
        ArrayDesign arrayDesign = new ArrayDesign(accession);
        arrayDesign.setName(name);
        arrayDesign.setProvider(provider);
        arrayDesign.setType(type);
        return arrayDesign;
    }

}
