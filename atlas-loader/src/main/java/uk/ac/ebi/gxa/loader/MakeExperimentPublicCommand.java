package uk.ac.ebi.gxa.loader;

public class MakeExperimentPublicCommand extends ExperimentEditorCommand {
    public MakeExperimentPublicCommand(String accession) {
        super(accession);
    }

    public void visit(AtlasLoaderCommandVisitor visitor) throws AtlasLoaderException {
        visitor.process(this);
    }
}
