package uk.ac.ebi.microarray.atlas.model;

public class Asset {
    private String name;
    private String fileName;
    private String description;

    public Asset(String name, String fileName, String description) {
        this.name = name;
        this.fileName = fileName;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Asset asset = (Asset) o;

        if (description != null ? !description.equals(asset.description) : asset.description != null) return false;
        if (fileName != null ? !fileName.equals(asset.fileName) : asset.fileName != null) return false;
        if (name != null ? !name.equals(asset.name) : asset.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
