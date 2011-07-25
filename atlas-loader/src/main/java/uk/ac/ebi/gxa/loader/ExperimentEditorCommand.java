package uk.ac.ebi.gxa.loader;

public abstract class ExperimentEditorCommand implements AtlasLoaderCommand {
    protected String accession;

    public ExperimentEditorCommand(String accession) {
        this.accession = accession;
    }

    public String getAccession() {
        return this.accession;
    }
}
