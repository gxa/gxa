package uk.ac.ebi.gxa.netcdf.migrator;

import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.utils.*;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreatorException;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreator;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.microarray.atlas.model.*;

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
    private AewDAO aewDAO;

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public void setAewDAO(AewDAO aewDAO) {
        this.aewDAO = aewDAO;
    }

    public AewDAO getAewDAO() {
        return aewDAO;
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

    public void generateNetCDFForAllExperiments(final boolean missingOnly) {

        log.info("Generating NetCDFs for all experiments from database" + (missingOnly ? " (only missing files)" : " (all files)"));
        ExecutorService service = Executors.newFixedThreadPool(getMaxThreads());
        Deque<Future> futures = new Deque<Future>(5);
        final List<Experiment> allExperiments = getAtlasDAO().getAllExperiments();
        log.info(allExperiments.size() + " found");
        for(final Experiment experiment : allExperiments) {
            futures.offerLast(service.submit(new Runnable() {
                public void run() {
                    try {
                        generateNetCDFForExperiment(experiment.getAccession(), missingOnly);
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
        arrayDesignCache.clear();
        service.shutdown();
    }

    private Map<String, ArrayDesign> arrayDesignCache = new WeakHashMap<String, ArrayDesign>();

    public void generateNetCDFForExperiment(String experimentAccession, boolean missingOnly) {
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
            ArrayDesign arrayDesign = arrayDesignCache.get(arrayDesignAccession);
            if(arrayDesign == null)
                arrayDesignCache.put(arrayDesignAccession, arrayDesign = getAtlasDAO().getArrayDesignByAccession(arrayDesignAccession));

            final File file = new File(getAtlasNetCDFRepo(), experiment.getExperimentID() + "_" + arrayDesign.getArrayDesignID() + ".nc");
            if(missingOnly && file.exists()) {
                log.info("Already exists, will not update");
                continue;
            }

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


            final DataMatrixStorage storage = new DataMatrixStorage(assays.size(), arrayDesign.getDesignElements().values().size() / 2, 1000);
            final boolean[] found = new boolean[] { false };
            log.info("Fetching expression values");
            getAewDAO().processExpressionValues(
                    experiment.getExperimentID(),
                    arrayDesign.getArrayDesignID(),
                    new ResultSetExtractor() {
                        public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                            String lastDE = null;
                            final Map<Long,Float> values = new HashMap<Long, Float>();
                            final Iterable<Float> valueMapper = new Iterable<Float>() {
                                public Iterator<Float> iterator() {
                                    return new MappingIterator<Assay, Float>(arrayDesignAssays.iterator()) {
                                        public Float map(Assay a) {
                                            Float v = values.get(a.getAssayID());
                                            return v != null ? v : -1000000f;
                                        }
                                    };
                                }
                            };
                            while(rs.next()) {
                                long assayId = rs.getLong(1);
                                String deAccession = rs.getString(2);
                                float value = rs.getFloat(3);
                                
                                if(lastDE == null) {
                                    lastDE = deAccession;
                                } else if(!lastDE.equals(deAccession)) {
                                    storage.add(lastDE, valueMapper.iterator());
                                    lastDE = deAccession;
                                    values.clear();
                                }

                                values.put(assayId, value);
                                found[0] = true;
                            }
                            if(!values.isEmpty() && lastDE != null)
                                storage.add(lastDE, valueMapper.iterator());

                            return null;
                        }
                    });


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

            log.info("Fetching expression values - done, now fetching analytics");

            Set<String> efs = new HashSet<String>();
            for (ObjectWithProperties assay : assays)
                for (Property prop : assay.getProperties())
                    efs.add(prop.getName());


            final EfvTree<Integer> efvTree = new EfvTree<Integer>();
            for (ObjectWithProperties assay : assays) {
                for (String propName : efs) {
                    StringBuilder propValue = new StringBuilder();
                    for (Property prop : assay.getProperties())
                        if (prop.getName().equals(propName)) {
                            if(propValue.length() > 0)
                                propValue.append(",");
                            propValue.append(prop.getValue());
                        }

                    efvTree.put(propName, propValue.toString(), 0);
                }
            }

            int efvNum = 0;
            for(EfvTree.EfEfv<Integer> e : efvTree.getNameSortedList())
                efvTree.put(e.getEf(), e.getEfv(), efvNum++);

            final DataMatrixStorage analyticsStorage = new DataMatrixStorage(efvNum * 2, storage.getSize(), 1000);
            found[0] = false;
            atlasDAO.getJdbcTemplate().query(
                    "SELECT de.accession, ef.name AS ef, efv.name AS efv, " +
                    "a.tstat, a.pvaladj " +
                    "FROM a2_expressionanalytics a " +
                    "JOIN a2_propertyvalue efv ON efv.propertyvalueid=a.propertyvalueid " +
                    "JOIN a2_property ef ON ef.propertyid=efv.propertyid " +
                    "JOIN a2_designelement de ON de.designelementid=a.designelementID " +
                    "WHERE a.experimentid=? AND de.arraydesignid=? ORDER BY de.accession, ef, efv",
                    new Object[] { experiment.getExperimentID(), arrayDesign.getArrayDesignID() },
                    new ResultSetExtractor() {
                        public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                            String lastDE = null;
                            Float[] values = new Float[analyticsStorage.getWidth()];
                            while(rs.next()) {
                                String deAccession = rs.getString(1);
                                String ef = rs.getString(2);
                                String efv = rs.getString(3);
                                Float tstat = rs.getFloat(4);
                                Float pval = rs.getFloat(5);

                                if(lastDE == null) {
                                    lastDE = deAccession;
                                } else if(!lastDE.equals(deAccession)) {
                                    analyticsStorage.add(lastDE, Arrays.asList(values).iterator());
                                    lastDE = deAccession;
                                    values = new Float[analyticsStorage.getWidth()];
                                }

                                Integer pos = efvTree.get(ef, efv);
                                if(pos != null) {
                                    values[pos] = tstat;
                                    values[pos + analyticsStorage.getWidth() / 2] = pval;
                                    found[0] = true;
                                }
                            }
                            if(lastDE != null)
                                analyticsStorage.add(lastDE, Arrays.asList(values).iterator());
                            return null;
                        }
                    }
            );

            if(found[0]) {
                Map<Pair<String,String>, DataMatrixStorage.ColumnRef> pvalMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
                Map<Pair<String,String>, DataMatrixStorage.ColumnRef> tstatMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
                for(EfvTree.EfEfv<Integer> e : efvTree.getNameSortedList()) {
                    tstatMap.put(new Pair<String, String>(e.getEf(), e.getEfv()), new DataMatrixStorage.ColumnRef(analyticsStorage, e.getPayload()));
                    pvalMap.put(new Pair<String, String>(e.getEf(), e.getEfv()), new DataMatrixStorage.ColumnRef(analyticsStorage, e.getPayload() + efvNum));
                }
                netCdfCreator.setTstatDataMap(tstatMap);
                netCdfCreator.setPvalDataMap(pvalMap);
            } else {
                log.info("No analytics found, won't write it");
            }

            log.info("Fetching analytics - done");

            netCdfCreator.setArrayDesign(arrayDesign);
            netCdfCreator.setExperiment(experiment);
            netCdfCreator.setVersion(version);

            try {
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
