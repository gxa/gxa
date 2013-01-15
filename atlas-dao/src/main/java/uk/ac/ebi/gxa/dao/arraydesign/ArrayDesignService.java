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

package uk.ac.ebi.gxa.dao.arraydesign;

import org.springframework.stereotype.Service;
import uk.ac.ebi.gxa.dao.ArrayDesignDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import java.io.IOException;

@Service
public class ArrayDesignService {

    private ArrayDesignDAO arrayDesignDAO;

    public ArrayDesignService(ArrayDesignDAO arrayDesignDAO) {
        this.arrayDesignDAO = arrayDesignDAO;
    }

    public ArrayDesign findOrCreateArrayDesignShallow(String accession) {
        try {
            return findOrCreateArrayDesignShallow(accession, true);
        } catch (IOException e) {
            //this should never happen because isMaster == true, so the SynonymsServiceClient is not being used
            throw new IllegalStateException(e);
        }
    }

    public ArrayDesign findOrCreateArrayDesignShallow(String accession, boolean isMaster) throws IOException{
        ArrayDesign arrayDesign = arrayDesignDAO.getArrayDesignShallowByAccession(accession);
        if (arrayDesign != null) {
            return arrayDesign;
        }
        return loadFromArrayExpress(accession, isMaster);
    }

    private ArrayDesign loadFromArrayExpress(String accession, boolean isMaster) throws IOException{
        ArrayExpressConnection aeConnection = new ArrayExpressConnection(accession);
        ArrayDesign arrayDesign = createNew(accession,
                                            isMaster ? null : new SynonymsServiceClient().fetchAccessionMaster(accession),
                                            aeConnection.getName(),
                                            aeConnection.getProvider(),
                                            aeConnection.getType());

        arrayDesignDAO.save(arrayDesign);
        return arrayDesign;
    }


    private ArrayDesign createNew(String accession, String accessionMaster, String name, String provider, String type) {
        ArrayDesign arrayDesign = new ArrayDesign(accession);
        arrayDesign.setName(name);
        arrayDesign.setAccessionMaster(accessionMaster);
        arrayDesign.setProvider(provider);
        arrayDesign.setType(type);
        return arrayDesign;
    }

}
