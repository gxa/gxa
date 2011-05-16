package uk.ac.ebi.microarray.atlas.model.bioentity;

/**
 * User: nsklyar
 * Date: 04/05/2011
 */
public class Software {
    private long softwareid;
    private String name;
    private String version;
    private String url;

    public Software(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public long getSoftwareid() {
        return softwareid;
    }

    public void setSoftwareid(long softwareid) {
        this.softwareid = softwareid;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Software software = (Software) o;

        if (name != null ? !name.equals(software.name) : software.name != null) return false;
        if (version != null ? !version.equals(software.version) : software.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }
}