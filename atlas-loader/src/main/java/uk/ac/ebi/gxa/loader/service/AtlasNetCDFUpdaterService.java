package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.UpdateNetCDFForExperimentCommand;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.utils.ValueListHashMap;
import uk.ac.ebi.gxa.utils.FilterIterator;
import uk.ac.ebi.gxa.utils.CountIterator;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreator;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreatorException;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;

/**
 * NetCDF updater service which preserves expression values information, but updates all properties
 * @author pashky
 */
public class AtlasNetCDFUpdaterService extends AtlasLoaderService<UpdateNetCDFForExperimentCommand> {

    public AtlasNetCDFUpdaterService(DefaultAtlasLoader atlasLoader) {
        super(atlasLoader);
    }

    public void process(UpdateNetCDFForExperimentCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        String experimentAccession = cmd.getAccession();

        listener.setAccession(experimentAccession);
        
        List<Assay> assays = getAtlasDAO().getAssaysByExperimentAccession(experimentAccession);

        ValueListHashMap<String, Assay> assaysByArrayDesign = new ValueListHashMap<String, Assay>();
        for(Assay assay : assays) {
            String adAcc = assay.getArrayDesignAccession();
            if(null != adAcc)
                assaysByArrayDesign.put(adAcc, assay);
        }

        Experiment experiment = getAtlasDAO().getExperimentByAccession(experimentAccession);
        final String version = "NetCDF Updater";

        for(String arrayDesignAccession : assaysByArrayDesign.keySet()) {
            ArrayDesign arrayDesign = getAtlasDAO().getArrayDesignByAccession(arrayDesignAccession);

            final File originalNetCDF = new File(getAtlasNetCDFRepo(), experiment.getExperimentID() + "_" + arrayDesign.getArrayDesignID() + ".nc");

            listener.setProgress("Reading existing NetCDF");
            final NetCDFProxy reader = new NetCDFProxy(originalNetCDF);

            final List<Assay> arrayDesignAssays = assaysByArrayDesign.get(arrayDesignAccession);
            getLog().info("Starting NetCDF for " + experimentAccession +
                    " and " + arrayDesignAccession + " (" + arrayDesignAssays.size() + " assays)");

            try {
                List<Assay> leaveAssays = new ArrayList<Assay>(arrayDesignAssays.size());
                final long[] oldAssays = reader.getAssays();
                for(int i = 0; i < oldAssays.length; ++i) {
                    for(Assay assay : arrayDesignAssays)
                        if(assay.getAssayID() == oldAssays[i]) {
                            leaveAssays.add(assay);
                            oldAssays[i] = -1; // mark it as used for later filtering
                            break;
                        }
                }


                String[] deAccessions = reader.getDesignElementAccessions();                
                DataMatrixStorage storage = new DataMatrixStorage(leaveAssays.size(), deAccessions.length, 1);
                for(int i = 0; i < deAccessions.length; ++i) {
                    final float[] values = reader.getExpressionDataForDesignElementAtIndex(i);
                    storage.add(deAccessions[i], new FilterIterator<Integer,Float>(CountIterator.zeroTo(values.length)) {
                        public Float map(Integer j) {
                            return oldAssays[j] == -1 ? values[j] : null; // skips deleted assays
                        }
                    });
                }                

                if(!originalNetCDF.delete())
                    throw new AtlasLoaderException("Can't delete original NetCDF file " + originalNetCDF);

                listener.setProgress("Writing new NetCDF");
                NetCDFCreator netCdfCreator = new NetCDFCreator();

                netCdfCreator.setAssays(leaveAssays);

                for (Assay assay : leaveAssays)
                    for (Sample sample : getAtlasDAO().getSamplesByAssayAccession(assay.getAccession()))
                        netCdfCreator.setSample(assay, sample);

                Map<String, DataMatrixStorage.ColumnRef> dataMap = new HashMap<String, DataMatrixStorage.ColumnRef>();
                for(int i = 0; i < leaveAssays.size(); ++i)
                    dataMap.put(leaveAssays.get(i).getAccession(), new DataMatrixStorage.ColumnRef(storage, i));

                netCdfCreator.setAssayDataMap(dataMap);

                netCdfCreator.setArrayDesign(arrayDesign);
                netCdfCreator.setExperiment(experiment);
                netCdfCreator.setVersion(version);

                netCdfCreator.createNetCdf(getAtlasNetCDFRepo());
                getLog().info("Successfully finished NetCDF for " + experimentAccession +
                        " and " + arrayDesignAccession);

            } catch (IOException e) {
                getLog().error("Error reading NetCDF for " + experimentAccession +
                        " and " + arrayDesignAccession);
                throw new AtlasLoaderException(e);
            } catch(NetCDFCreatorException e) {
                getLog().error("Error writing NetCDF for " + experimentAccession +
                        " and " + arrayDesignAccession);
                throw new AtlasLoaderException(e);
            }
        }
    }
}
