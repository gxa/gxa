package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.Asset;

/**
 * @author Misha Kapushesky
 */
public class ApiAsset {
    private String name;
    private String fileName;
    private String description;

    public ApiAsset() {}

    public ApiAsset(final String name, final String fileName, final String description) {
        this.name = name;
        this.fileName = fileName;
        this.description = description;
    }

    public ApiAsset(final Asset asset) {
        this.name = asset.getName();
        this.fileName = asset.getFileName();
        this.description = asset.getDescription();
    }
}
