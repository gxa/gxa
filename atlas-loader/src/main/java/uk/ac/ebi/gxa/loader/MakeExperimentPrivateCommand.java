package uk.ac.ebi.gxa.loader;

public class MakeExperimentPrivateCommand extends ExperimentEditorCommand {
    public MakeExperimentPrivateCommand(String accession) {
        super(accession);
    }

    public void visit(AtlasLoaderCommandVisitor visitor) throws AtlasLoaderException {
        visitor.process(this);
    }
}
