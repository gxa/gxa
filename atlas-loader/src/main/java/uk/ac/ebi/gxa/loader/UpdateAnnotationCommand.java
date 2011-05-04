package uk.ac.ebi.gxa.loader;

/**
 * User: nsklyar
 * Date: 21/04/2011
 */
public class UpdateAnnotationCommand extends AbstractAccessionCommand{

    public UpdateAnnotationCommand(String accession) {
        super(accession);
    }

    @Override
    public void visit(AtlasLoaderCommandVisitor visitor) throws AtlasLoaderException {
        visitor.process(this);
    }
}
