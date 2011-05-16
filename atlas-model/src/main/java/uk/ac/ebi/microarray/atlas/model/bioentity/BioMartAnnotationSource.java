package uk.ac.ebi.microarray.atlas.model.bioentity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
public class BioMartAnnotationSource extends AnnotationSource{

    private String url;
    private Map<String, String> martToAtlasProperties = new HashMap<String, String>();

    public BioMartAnnotationSource(String name, String version) {
        super(name, version);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getMartToAtlasProperties() {
        return Collections.unmodifiableMap(martToAtlasProperties);
    }

    public void addMartProperty(String martProperty, String atlasProperty) {
        martToAtlasProperties.put(martProperty, atlasProperty);
    }

    private String getAtlasProperty(String martProperty) {
        return martToAtlasProperties.get(martProperty);
    }


}
