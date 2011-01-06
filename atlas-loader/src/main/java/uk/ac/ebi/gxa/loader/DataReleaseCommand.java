package uk.ac.ebi.gxa.loader;

public class DataReleaseCommand implements AtlasLoaderCommand {
    private String accession;

    public DataReleaseCommand(String accession) {
        this.accession = accession;
    }

    public void visit(AtlasLoaderCommandVisitor visitor) throws AtlasLoaderException {
        visitor.process(this);
    }

    public String getAccession() {
        return this.accession;
    }
}
