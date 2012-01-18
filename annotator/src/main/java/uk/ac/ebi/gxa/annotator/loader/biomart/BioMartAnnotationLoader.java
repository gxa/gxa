package uk.ac.ebi.gxa.annotator.loader.biomart;

import uk.ac.ebi.gxa.annotator.loader.util.InvalidCSVColumnException;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityData;
import uk.ac.ebi.gxa.annotator.loader.data.DesignElementMappingData;
import uk.ac.ebi.gxa.annotator.loader.data.InvalidAnnotationDataException;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalArrayDesign;
import uk.ac.ebi.gxa.annotator.model.ExternalBioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author Olga Melnichuk
 * @version 1/16/12 1:23 PM
 */
public class BioMartAnnotationLoader implements Closeable {

    private final MartServiceClient martClient;

    private final BioMartAnnotationSource annotSource;

    private BioEntityData.Builder entityBuilder;

    private BioEntityAnnotationData.Builder pvBuilder;

    private DesignElementMappingData.Builder deMappingsBuilder;

    public BioMartAnnotationLoader(BioMartAnnotationSource annotSource) throws URISyntaxException {
        this.martClient = MartServiceClient.create(annotSource);
        this.annotSource = annotSource;
    }

    public void loadBioEntities() throws BioMartException, IOException, InvalidCSVColumnException {
        entityBuilder = new BioEntityData.Builder(annotSource.getOrganism());
        (new MartBioEntitiesLoader(annotSource, martClient)).load(entityBuilder);
    }

    public void loadPropertyValues(ExternalBioEntityProperty externalProperty)
            throws BioMartException, IOException, InvalidCSVColumnException {
        pvBuilder = pvBuilder == null ? new BioEntityAnnotationData.Builder() : pvBuilder;
        (new MartPropertyValuesLoader(annotSource, martClient)).load(externalProperty, pvBuilder);
    }

    public void loadSynonyms(BioEntityProperty synonymProperty) throws BioMartException {
        pvBuilder = pvBuilder == null ? new BioEntityAnnotationData.Builder() : pvBuilder;
        (new MartSynonymPropertyValuesLoader(annotSource)).load(synonymProperty, pvBuilder);
    }

    public void loadDesignElementMappings(ExternalArrayDesign externalArrayDesign)
            throws BioMartException, IOException, InvalidCSVColumnException {
        deMappingsBuilder = new DesignElementMappingData.Builder();
        (new MartDesignElementMappingsLoader(annotSource, martClient)).load(externalArrayDesign, deMappingsBuilder);
    }

    public BioEntityData getBioEntityData() throws InvalidAnnotationDataException {
        return entityBuilder == null ? null : entityBuilder.build(annotSource.getTypes());
    }

    public BioEntityAnnotationData getPropertyValuesData() throws InvalidAnnotationDataException {
        return pvBuilder == null ? null : pvBuilder.build(annotSource.getTypes());
    }

    public DesignElementMappingData getDeMappingsData() throws InvalidAnnotationDataException {
        return deMappingsBuilder == null ? null : deMappingsBuilder.build(annotSource.getTypes());
    }

    @Override
    public void close() throws IOException {
        martClient.close();
    }
}
