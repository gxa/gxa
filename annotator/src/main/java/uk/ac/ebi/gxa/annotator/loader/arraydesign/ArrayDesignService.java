package uk.ac.ebi.gxa.annotator.loader.arraydesign;

import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.dao.ArrayDesignDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

/**
 * User: nsklyar
 * Date: 02/09/2011
 */
public class ArrayDesignService {

    private ArrayDesignDAO arrayDesignDAO;

    @Autowired
    private ArrayExpressConnection arrayExpressConnection;

    public ArrayDesignService(ArrayDesignDAO arrayDesignDAO) {
        this.arrayDesignDAO = arrayDesignDAO;
    }

    public ArrayDesign findOrCreateArrayDesignShallow(String accession) {
        ArrayDesign arrayDesign = arrayDesignDAO.getArrayDesignShallowByAccession(accession);
        if (arrayDesign == null) {
            arrayDesign = fetchDataFromAE(accession);
        } 
        if (arrayDesign == null) {
            arrayDesign = createNew(accession);
        }
        arrayDesignDAO.save(arrayDesign);
        return arrayDesign;
    }

    private ArrayDesign fetchDataFromAE(String accession) {
        ArrayDesign arrayDesign = arrayExpressConnection.fetchArrayDesignData(accession);
        return arrayDesign;
    }

    protected ArrayDesign createNew(String accession) {
        ArrayDesign arrayDesign = new ArrayDesign(accession);
        arrayDesign.setName("Auto generated Array Design for accession " + accession);
        return arrayDesign;
    }
 
}
