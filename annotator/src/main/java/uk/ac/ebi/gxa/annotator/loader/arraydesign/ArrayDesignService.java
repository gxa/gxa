package uk.ac.ebi.gxa.annotator.loader.arraydesign;

import org.apache.commons.lang.StringUtils;
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
        ArrayDesign arrayDesign = arrayDesignDAO.getArrayDesignShallowByAccession(accession);
        if (arrayDesign == null) {
            ArrayExpressConnection aeConnection = new ArrayExpressConnection(accession);
            arrayDesign = createNew(accession,
                    aeConnection.getName(),
                    aeConnection.getProvider(),
                    aeConnection.getType());
        }

        arrayDesignDAO.save(arrayDesign);
        return arrayDesign;
    }

    private ArrayDesign createNew(String accession, String name, String provider, String type) {
        ArrayDesign arrayDesign = new ArrayDesign(accession);

        if (StringUtils.isNotEmpty(name)) {
            arrayDesign.setName(name);
        } else {
            arrayDesign.setName("Auto generated Array Design for accession " + accession);
        }

        arrayDesign.setProvider(provider);
        arrayDesign.setType(type);
        return arrayDesign;
    }

}
