package uk.ac.ebi.gxa.loader;

/**
 * User: nsklyar
 * Date: 21/04/2011
 */
public class BioMartUpdateCommand extends AbstractAccessionCommand{

    private BioMartUpdateType updateType;

    public BioMartUpdateCommand(String accession, BioMartUpdateType type) {
        super(accession);
        this.updateType = type;
    }

    @Override
    public void visit(AtlasLoaderCommandVisitor visitor) throws AtlasLoaderException {
        visitor.process(this);
    }

    public BioMartUpdateType getUpdateType() {
        return updateType;
    }

    public static enum BioMartUpdateType {
        ANNOTATIONS,
        MAPPINGS;
    }
}
