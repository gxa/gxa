package uk.ac.ebi.gxa.netcdf.migrator;

import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.utils.ValueListHashMap;
import uk.ac.ebi.gxa.utils.MappingIterator;
import uk.ac.ebi.gxa.utils.Deque;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreatorException;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreator;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.dao.DataAccessException;

/**
 * @author pashky
 */
public class DefaultNetCDFMigrator implements AtlasNetCDFMigrator {
    private static Logger log = LoggerFactory.getLogger(DefaultNetCDFMigrator.class);

    private AtlasDAO atlasDAO;
    private File atlasNetCDFRepo;
    private int maxThreads;

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public File getAtlasNetCDFRepo() {
        return atlasNetCDFRepo;
    }

    public void setAtlasNetCDFRepo(File atlasNetCDFRepo) {
        this.atlasNetCDFRepo = atlasNetCDFRepo;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public void generateNetCDFForAllExperiments() {

        log.info("Generating NetCDFs for all experiments from database");
        ExecutorService service = Executors.newFixedThreadPool(getMaxThreads());
        Deque<Future> futures = new Deque<Future>(5);
        final List<Experiment> allExperiments = getAtlasDAO().getAllExperiments();
        log.info(allExperiments.size() + " found");
        for(final Experiment experiment : allExperiments) {
            futures.offerLast(service.submit(new Runnable() {
                public void run() {
                    try {
                        generateNetCDFForExperiment(experiment.getAccession());
                    } catch(RuntimeException e) {
                        log.error("Exception", e);
                        throw e;
                    }
                }
            }));
        }

        while(true) {
            Future f = futures.poll();
            if(f == null)
                break;
            try {
                while(!f.isDone())
                    try {
                        f.get();
                    } catch(InterruptedException e) {
                        //
                    }
            } catch(ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("Shutting down");
        service.shutdown();
    }

    public void generateNetCDFForExperiment(String experimentAccession) {
        List<Assay> assays = getAtlasDAO().getAssaysByExperimentAccession(experimentAccession);

        ValueListHashMap<String, Assay> assaysByArrayDesign = new ValueListHashMap<String, Assay>();
        for(Assay assay : assays) {
            String adAcc = assay.getArrayDesignAccession();
            if(null != adAcc)
                assaysByArrayDesign.put(adAcc, assay);
        }

        Experiment experiment = getAtlasDAO().getExperimentByAccession(experimentAccession);
        final String version = "NetCDF Migrator";

        for(String arrayDesignAccession : assaysByArrayDesign.keySet()) {
            NetCDFCreator netCdfCreator = new NetCDFCreator();

            final List<Assay> arrayDesignAssays = assaysByArrayDesign.get(arrayDesignAccession);
            log.info("Starting NetCDF for " + experimentAccession +
                    " and " + arrayDesignAccession + " (" + arrayDesignAssays.size() + " assays)");

            Collections.sort(arrayDesignAssays, new Comparator<Assay>() {
                public int compare(Assay o1, Assay o2) {
                    return Long.valueOf(o1.getAssayID()).compareTo(o2.getAssayID());
                }
            });

            netCdfCreator.setAssays(arrayDesignAssays);

            for (Assay assay : arrayDesignAssays)
                for (Sample sample : getAtlasDAO().getSamplesByAssayAccession(assay.getAccession()))
                    netCdfCreator.setSample(assay, sample);

            ArrayDesign arrayDesign = getAtlasDAO().getArrayDesignByAccession(arrayDesignAccession);

            final DataMatrixStorage storage = new DataMatrixStorage(assays.size(), arrayDesign.getDesignElements().values().size() / 2, 1000);
            final boolean[] found = new boolean[] { false };
            log.info("Fetching expression values");
            getAtlasDAO().getJdbcTemplate().query(
                    "SELECT ev.assayid, de.accession, ev.value " +
                            "FROM A2_Expressionvalue ev " +
                            "JOIN a2_assay a ON a.assayid = ev.assayid " +
                            "JOIN a2_designelement de ON de.designelementid = ev.designelementid " +
                            "WHERE a.experimentid=? AND a.arraydesignid=? ORDER BY de.accession, ev.assayid",
                    new Object[] {
                            experiment.getExperimentID(),
                            arrayDesign.getArrayDesignID()
                    },
                    new ResultSetExtractor() {
                        public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                            String lastDE = null;
                            final Map<Long,Float> values = new HashMap<Long, Float>();
                            final MappingIterator<Assay, Float> valueMapper = new MappingIterator<Assay, Float>(arrayDesignAssays.iterator()) {
                                public Float map(Assay a) {
                                    Float v = values.get(a.getAssayID());
                                    return v != null ? v : -1000000f;
                                }
                            };
                            while(rs.next()) {
                                long assayId = rs.getLong(1);
                                String deAccession = rs.getString(2);
                                float value = rs.getFloat(3);
                                
                                if(lastDE == null) {
                                    lastDE = deAccession;
                                } else if(!lastDE.equals(deAccession)) {
                                    storage.add(lastDE, valueMapper);
                                    lastDE = deAccession;
                                    values.clear();
                                }

                                values.put(assayId, value);
                                found[0] = true;
                            }
                            if(!values.isEmpty() && lastDE != null)
                                storage.add(lastDE, valueMapper);

                            return null;
                        }
                    });

            log.info("Fetching expression values - done");

            if(!found[0]) {
                log.warn("No expression values found in database, creating empty NetCDF");
                storage.add("dummy", new MappingIterator<Assay, Float>(arrayDesignAssays.iterator()) {
                    public Float map(Assay a) {
                        return -1000000f;
                    }
                });
            }
            
            Map<String, DataMatrixStorage.ColumnRef> dataMap = new HashMap<String, DataMatrixStorage.ColumnRef>();
            int i = 0;
            for(Assay assay : arrayDesignAssays)
                dataMap.put(assay.getAccession(), new DataMatrixStorage.ColumnRef(storage, i++));

            netCdfCreator.setAssayDataMap(dataMap);

            netCdfCreator.setArrayDesign(arrayDesign);
            netCdfCreator.setExperiment(experiment);
            netCdfCreator.setVersion(version);

            try {
                log.info("File is " + new File(getAtlasNetCDFRepo(), experiment.getExperimentID() + "_" + arrayDesign.getArrayDesignID() + ".nc"));
                netCdfCreator.createNetCdf(getAtlasNetCDFRepo());
                log.info("Successfully finished NetCDF for " + experimentAccession +
                        " and " + arrayDesignAccession);
            } catch(NetCDFCreatorException e) {
                log.info("Error writing NetCDF for " + experimentAccession +
                        " and " + arrayDesignAccession);

            }
        }
    }
}
