package uk.ac.ebi.gxa.service.export;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityDAO;

import java.lang.reflect.Type;
import java.util.Map;

public class EnsemblIDGeneNameMappingExporter {

    @Autowired
    private BioEntityDAO bioEntityDAO;


    public String generateDataAsString(String organismName) {

        Map<String, String> geneNames = bioEntityDAO.getGeneNames(organismName);
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, String>>(){}.getType();
        return gson.toJson(geneNames, mapType);
    }

    public void setBioEntityDAO(BioEntityDAO bioEntityDAO) {
        this.bioEntityDAO = bioEntityDAO;
    }
}
