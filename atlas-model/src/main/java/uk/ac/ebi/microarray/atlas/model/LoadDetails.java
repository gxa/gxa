package uk.ac.ebi.microarray.atlas.model;

/**
 * Class representing the statistics about experiments and genes stored in the database.  On loading, the details of any
 * given experiment should be populated with the experiment accession or the gene identifier, and it's status set to
 * "loading".  The various flags indicate whether the entity referenced has been indexed, used to populate the index,
 * had analytics calculated, and so on.
 *
 * @author Tony Burdett
 * @date 13-Nov-2009
 */
public class LoadDetails {
    private String accession;
    private String status;
    private String netCDF;
    private String similarity;
    private String ranking;
    private String searchIndex;
    private String loadType;

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNetCDF() {
        return netCDF;
    }

    public void setNetCDF(String netCDF) {
        this.netCDF = netCDF;
    }

    public String getSimilarity() {
        return similarity;
    }

    public void setSimilarity(String similarity) {
        this.similarity = similarity;
    }

    public String getRanking() {
        return ranking;
    }

    public void setRanking(String ranking) {
        this.ranking = ranking;
    }

    public String getSearchIndex() {
        return searchIndex;
    }

    public void setSearchIndex(String searchIndex) {
        this.searchIndex = searchIndex;
    }

    public String getLoadType() {
        return loadType;
    }

    public void setLoadType(String loadType) {
        this.loadType = loadType;
    }
}
