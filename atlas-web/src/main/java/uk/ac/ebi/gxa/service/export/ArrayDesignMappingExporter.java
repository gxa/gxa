package uk.ac.ebi.gxa.service.export;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.dao.ArrayDesignDAO;

import java.lang.reflect.Type;
import java.util.Map;

public class ArrayDesignMappingExporter {

    @Autowired
    private ArrayDesignDAO arrayDesignDAO;


    public String generateDataAsString(String arrayDesignAccession) {

        Map<String,String> designElementGeneAccMapping = arrayDesignDAO.getDesignElementGeneAccMapping(arrayDesignAccession);
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, String>>(){}.getType();
        return gson.toJson(designElementGeneAccMapping, mapType);
    }

    protected void setArrayDesignDAO(ArrayDesignDAO arrayDesignDAO) {
        this.arrayDesignDAO = arrayDesignDAO;
    }
}
