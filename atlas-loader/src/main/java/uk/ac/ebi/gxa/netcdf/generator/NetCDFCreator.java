/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */
package uk.ac.ebi.gxa.netcdf.generator;

import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.utils.FlattenIterator;
import uk.ac.ebi.gxa.utils.MappingIterator;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.*;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Efficient NetCDF writer tailored to handle chunked expression values blocks found in
 * MAGETAB expression matrixes
 *
 * @author pashky
 */
public class NetCDFCreator {
    private final Logger log = LoggerFactory.getLogger(NetCDFCreator.class);

    private Experiment experiment;
    private ArrayDesign arrayDesign;

    private List<Assay> assays;
    private LinkedHashSet<Sample> samples = new LinkedHashSet<Sample>();
    private ListMultimap<Assay, Sample> samplesMap = ArrayListMultimap.create();
    private Map<String, DataMatrixStorage.ColumnRef> assayDataMap = new HashMap<String, DataMatrixStorage.ColumnRef>();
    private List<String> assayAccessions;
    private List<String> sampleAccessions;
    private ListMultimap<String, String> scvOntologies;
    private ListMultimap<String, String> efvOntologies;
    private int maxAssayLength;
    private int maxSampleLength;
    private int maxEfvoLength;
    private int maxScvoLength;

    private List<DataMatrixStorage> storages = new ArrayList<DataMatrixStorage>();
    private ListMultimap<DataMatrixStorage, Assay> storageAssaysMap = ArrayListMultimap.create();

    private Iterable<String> mergedDesignElements;
    private Map<String, Integer> mergedDesignElementsMap;
    private boolean canWriteFirstFull;

    // maps of properties
    private Multimap<String, String> efvMap;
    private Multimap<String, String> scvMap;
    private LinkedHashSet<String> efScs; // efs/scs
    private Multimap<String, String> propertyToUnsortedUniqueValues = LinkedHashMultimap.create(); // sc/ef -> unsorted scvs/efvs
    private Map<String, List<String>> propertyToSortedUniqueValues = new LinkedHashMap<String, List<String>>(); // sc/ef -> sorted scs/efvs

    private Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalDataMap; // stores pvals across all unique ecvs/efvs
    private Map<Pair<String, String>, DataMatrixStorage.ColumnRef> tstatDataMap; // stores tstats across all unique ecvs/efvs

    private List<String> warnings = new ArrayList<String>();

    private NetcdfFileWriteable netCdf;

    private int totalDesignElements;
    private int totalUniqueValues; // scvs/efvs
    private int maxDesignElementLength;
    private int maxEfLength;
    private int maxEfScLength;
    private int maxEfvLength;
    private int maxScLength;
    private int maxScvLength;

    private String version = "0.0";

    public void setVersion(String version) {
        this.version = version;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public void setArrayDesign(ArrayDesign arrayDesign) {
        this.arrayDesign = arrayDesign;
    }

    public void setAssays(List<Assay> assays) {
        this.assays = assays;
    }

    public void setSample(Assay assay, Sample sample) {
        this.samples.add(sample);
        this.samplesMap.put(assay, sample);
    }

    public void setAssayDataMap(Map<String, DataMatrixStorage.ColumnRef> assayDataMap) {
        this.assayDataMap = assayDataMap;
    }

    public void setPvalDataMap(Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalDataMap) {
        this.pvalDataMap = pvalDataMap;
    }

    public void setTstatDataMap(Map<Pair<String, String>, DataMatrixStorage.ColumnRef> tstatDataMap) {
        this.tstatDataMap = tstatDataMap;
    }

    private ListMultimap<String, String> extractProperties(List<? extends ObjectWithProperties> objects) {
        ListMultimap<String, String> result = ArrayListMultimap.create();
        for (ObjectWithProperties o : objects) {
            for (String name : o.getPropertyNames()) {
                result.put(name, o.getPropertySummary(name));
            }
        }
        return result;
    }

    private static ListMultimap<String, String> extractOntologies(List<? extends ObjectWithProperties> objects) {
        ListMultimap<String, String> result = ArrayListMultimap.create();
        for (ObjectWithProperties o : objects) {
            for (String name : o.getPropertyNames()) {
                result.put(name, o.getEfoSummary(name));
            }
        }
        return result;
    }

    public void prepareData() {
        for (Assay a : assays) {
            DataMatrixStorage buf = assayDataMap.get(a.getAccession()).storage;
            if (!storages.contains(buf))
                storages.add(buf);
        }

        if (pvalDataMap != null)
            for (DataMatrixStorage.ColumnRef ref : pvalDataMap.values()) {
                if (!storages.contains(ref.storage))
                    storages.add(ref.storage);
            }

        if (tstatDataMap != null)
            for (DataMatrixStorage.ColumnRef ref : tstatDataMap.values()) {
                if (!storages.contains(ref.storage))
                    storages.add(ref.storage);
            }

        // sort assay in order of buffers and reference numbers in those buffers
        Collections.sort(assays, new Comparator<Assay>() {
            public int compare(Assay o1, Assay o2) {
                DataMatrixStorage.ColumnRef ref1 = assayDataMap.get(o1.getAccession());
                DataMatrixStorage buf1 = ref1.storage;
                int i1 = storages.indexOf(buf1);

                DataMatrixStorage.ColumnRef ref2 = assayDataMap.get(o2.getAccession());
                DataMatrixStorage buf2 = ref2.storage;
                int i2 = storages.indexOf(buf2);

                if (i1 != i2)
                    return i1 - i2;

                return ref1.referenceIndex - ref2.referenceIndex;
            }
        });

        // build reverse map
        for (Assay a : assays)
            storageAssaysMap.put(assayDataMap.get(a.getAccession()).storage, a);

        // reshape available properties to match assays & samples
        final List<ObjectWithProperties> samplesList = new ArrayList<ObjectWithProperties>(samples);
        efvMap = extractProperties(assays);
        scvMap = extractProperties(samplesList);

        // Merge efvMap and scvMap into propertyToUnsortedUniqueValues that will store all scv/efv properties
        for (Map.Entry<String, Collection<String>> efToEfvs : efvMap.asMap().entrySet()) {
            propertyToUnsortedUniqueValues.putAll(efToEfvs.getKey(), efToEfvs.getValue());
        }
        for (Map.Entry<String, Collection<String>> scToScvs : scvMap.asMap().entrySet()) {
            propertyToUnsortedUniqueValues.putAll(scToScvs.getKey(), scToScvs.getValue());
        }

        efScs = getEfScs(efvMap, scvMap);
        efvOntologies = extractOntologies(assays);
        scvOntologies = extractOntologies(samplesList);

        // find maximum lengths for ef/efv/sc/scv strings
        maxEfLength = 0;
        maxEfScLength = 0;
        maxEfvLength = 0;
        maxEfvoLength = 0;
        for (String ef : efvMap.keySet()) {
            maxEfLength = Math.max(maxEfLength, ef.length());
            maxEfScLength = Math.max(maxEfScLength, ef.length());
            for (String efv : efvMap.get(ef))
                maxEfvLength = Math.max(maxEfvLength, efv.length());
            for (String efvo : efvOntologies.get(ef))
                maxEfvoLength = Math.max(maxEfvoLength, efvo.length());
        }

        maxScLength = 0;
        maxScvLength = 0;
        maxScvoLength = 0;
        for (String sc : scvMap.keySet()) {
            maxScLength = Math.max(maxScLength, sc.length());
            maxEfScLength = Math.max(maxEfScLength, sc.length());
            for (String scv : scvMap.get(sc))
                maxScvLength = Math.max(maxScvLength, scv.length());
            for (String scvo : scvOntologies.get(sc))
                maxScvoLength = Math.max(maxScvoLength, scvo.length());
        }

        totalUniqueValues = populateUniqueValues(propertyToUnsortedUniqueValues, propertyToSortedUniqueValues, pvalDataMap);

        // merge available design elements (if needed)
        canWriteFirstFull = true;

        if (storages.size() == 1)
            mergedDesignElements = storages.get(0).getDesignElements();
        else {
            mergedDesignElementsMap = new LinkedHashMap<String, Integer>();
            boolean first = true;
            for (DataMatrixStorage buffer : storages) {
                for (String de : buffer.getDesignElements())
                    if (!mergedDesignElementsMap.containsKey(de))
                        mergedDesignElementsMap.put(de, mergedDesignElementsMap.size());
                    else if (first)
                        canWriteFirstFull = false;

                first = false;
            }
            mergedDesignElements = mergedDesignElementsMap.keySet();
        }

        // calculate number of available DEs, genes and maximum accesion string length
        totalDesignElements = 0;
        maxDesignElementLength = 0;

        for (String de : mergedDesignElements) {
            maxDesignElementLength = Math.max(maxDesignElementLength, de.length());
            ++totalDesignElements;
        }
        assayAccessions = new ArrayList<String>();
        sampleAccessions = new ArrayList<String>();
        maxAssayLength = 0;
        maxSampleLength = 0;


        for (Assay assay : assays) {
            assayAccessions.add(assay.getAccession());
            maxAssayLength = Math.max(maxAssayLength, assay.getAccession().length());
        }
        for (Sample sample : samples) {
            sampleAccessions.add(sample.getAccession());
            maxSampleLength = Math.max(maxSampleLength, sample.getAccession().length());
        }
    }

    /**
     *
     * @param unsortedUniqueValueMap source of non-unique data from which sortedUniqueValueMap is populated;
     *                               ef or sc -> list of non-unique efvs/scvs corresponding to ef/sc key respectively
     * @param sortedUniqueValueMap   populated by this method; ef or sc -> list of unique efvs/scvs corresponding to ef/sc key respectively
     * @param pvalDataMap            Map: ef-efv or sc-scv -> DataMatrixStorage.ColumnRef
     * @return total number of unique values in uniqueValueMap
     */
    private int populateUniqueValues(
            final Multimap<String, String> unsortedUniqueValueMap,
            final Map<String, List<String>> sortedUniqueValueMap,
            final Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalDataMap
    ) {
        int totalUniqueValues = 0;
        for (final Map.Entry<String, Collection<String>> efOrSc : unsortedUniqueValueMap.asMap().entrySet()) {
            List<String> efvsOrScvs = new ArrayList<String>(new HashSet<String>(efOrSc.getValue()));
            if (pvalDataMap != null) {
                Collections.sort(efvsOrScvs, new Comparator<String>() {
                    public int compare(String o1, String o2) {
                        DataMatrixStorage.ColumnRef ref1 = pvalDataMap.get(Pair.create(efOrSc.getKey(), o1));
                        DataMatrixStorage buf1 = ref1.storage;
                        int i1 = storages.indexOf(buf1);

                        DataMatrixStorage.ColumnRef ref2 = pvalDataMap.get(Pair.create(efOrSc.getKey(), o2));
                        DataMatrixStorage buf2 = ref2.storage;
                        int i2 = storages.indexOf(buf2);

                        if (i1 != i2)
                            return i1 - i2;

                        return ref1.referenceIndex - ref2.referenceIndex;
                    }
                });
            } else
                Collections.sort(efvsOrScvs);
            sortedUniqueValueMap.put(efOrSc.getKey(), efvsOrScvs);
            totalUniqueValues += sortedUniqueValueMap.get(efOrSc.getKey()).size();

        }
        return totalUniqueValues;
    }

    private void create() throws IOException {
        // NetCDF doesn't know how to store longs, so we use DataType.DOUBLE for internal DB ids

        Dimension assayDimension = netCdf.addDimension("AS", assays.size());
        netCdf.addVariable("AS", DataType.DOUBLE, new Dimension[]{assayDimension});

        Dimension sampleDimension = netCdf.addDimension("BS", samples.size());
        netCdf.addVariable("BS", DataType.DOUBLE, new Dimension[]{sampleDimension});

        netCdf.addVariable("BS2AS", DataType.INT,
                new Dimension[]{sampleDimension, assayDimension});

        // update the netCDF with the genes count
        Dimension designElementDimension = netCdf.addDimension("DE", totalDesignElements);
        Dimension designElementLenDimension = netCdf.addDimension("DElen", maxDesignElementLength);

        netCdf.addVariable("DEacc", DataType.CHAR, new Dimension[]{designElementDimension, designElementLenDimension});
        netCdf.addVariable("DE", DataType.DOUBLE, new Dimension[]{designElementDimension});
        netCdf.addVariable("GN", DataType.DOUBLE, new Dimension[]{designElementDimension});

        //accessions for Assays and Samples
        Dimension assayLenDimension = netCdf.addDimension("ASlen", maxAssayLength);
        netCdf.addVariable("ASacc", DataType.CHAR, new Dimension[]{assayDimension, assayLenDimension});

        Dimension sampleLenDimension = netCdf.addDimension("BSlen", maxSampleLength);
        netCdf.addVariable("BSacc", DataType.CHAR, new Dimension[]{sampleDimension, sampleLenDimension});

        if (!scvMap.isEmpty() || !efvMap.isEmpty()) {
            Dimension efscDimension = netCdf.addDimension("EFSC", efScs.size());
            Dimension efScLenDimension = netCdf.addDimension("EFSClen", maxEfScLength);
            netCdf.addVariable("EFSC", DataType.CHAR, new Dimension[]{efscDimension, efScLenDimension});

            if (!scvMap.isEmpty()) {
                Dimension scDimension = netCdf.addDimension("SC", scvMap.keySet().size());
                Dimension sclenDimension = netCdf.addDimension("SClen", maxScLength);

                netCdf.addVariable("SC", DataType.CHAR, new Dimension[]{scDimension, sclenDimension});

                Dimension scvlenDimension = netCdf.addDimension("SCVlen", maxScvLength);
                netCdf.addVariable("SCV", DataType.CHAR, new Dimension[]{scDimension, sampleDimension, scvlenDimension});

                if (maxScvoLength > 0) {
                    Dimension scvolenDimension = netCdf.addDimension("SCVOlen", maxScvoLength);
                    netCdf.addVariable("SCVO", DataType.CHAR, new Dimension[]{scDimension, sampleDimension, scvolenDimension});
                }
            }

            if (!efvMap.isEmpty()) {
                Dimension efDimension = netCdf.addDimension("EF", efvMap.keySet().size());
                Dimension eflenDimension = netCdf.addDimension("EFlen", maxEfLength);

                netCdf.addVariable("EF", DataType.CHAR, new Dimension[]{efDimension, eflenDimension});

                Dimension efvlenDimension = netCdf.addDimension("EFVlen", maxEfLength + maxEfvLength + 2);
                netCdf.addVariable("EFV", DataType.CHAR, new Dimension[]{efDimension, assayDimension, efvlenDimension});

                if (maxEfvoLength > 0) {
                    Dimension efvolenDimension = netCdf.addDimension("EFVOlen", maxEfvoLength);
                    netCdf.addVariable("EFVO", DataType.CHAR, new Dimension[]{efDimension, assayDimension, efvolenDimension});
                }
            }

            // Now add unique values and stats dimensions
            Dimension uvalDimension = netCdf.addDimension("uVAL", totalUniqueValues);

            Dimension uValLenDimension = netCdf.addDimension("uVALlen", maxEfScLength + Math.max(maxEfvLength, maxScvLength) + 2);
            netCdf.addVariable("uVAL", DataType.CHAR, new Dimension[]{uvalDimension, uValLenDimension});
            netCdf.addVariable("uVALnum", DataType.INT, new Dimension[]{efscDimension});

            netCdf.addVariable("PVAL", DataType.FLOAT, new Dimension[]{designElementDimension, uvalDimension});
            netCdf.addVariable("TSTAT", DataType.FLOAT, new Dimension[]{designElementDimension, uvalDimension});

            String[] sortOrders = new String[]{"ANY", "UP_DOWN", "UP", "DOWN", "NON_D_E"};
            for (String orderName : sortOrders) {
                netCdf.addVariable("ORDER_" + orderName, DataType.INT, new Dimension[]{designElementDimension});
            }
        }

        netCdf.addVariable("BDC", DataType.FLOAT, new Dimension[]{designElementDimension, assayDimension});

        // add metadata global attributes
        safeAddGlobalAttribute(
                "CreateNetCDF_VERSION",
                version);
        safeAddGlobalAttribute(
                "experiment_accession",
                experiment.getAccession());
        safeAddGlobalAttribute(
                "ADaccession",
                arrayDesign.getAccession());
        safeAddGlobalAttribute(
                "ADid",
                (double) arrayDesign.getArrayDesignID()); // netcdf doesn't know how to store longs
        safeAddGlobalAttribute(
                "ADname",
                arrayDesign.getName());
        safeAddGlobalAttribute(
                "experiment_lab",
                experiment.getLab());
        safeAddGlobalAttribute(
                "experiment_performer",
                experiment.getPerformer());
        safeAddGlobalAttribute(
                "experiment_pmid",
                experiment.getPubmedID());
        safeAddGlobalAttribute(
                "experiment_abstract",
                experiment.getArticleAbstract());

        netCdf.create();
    }

    private void write() throws IOException, InvalidRangeException {
        writeSamplesAssays();
        writeSampleAccessions();
        writeAssayAccessions();
        writeAssayOntologies();
        writeSampleOntologies();

        if (!efvMap.isEmpty())
            writeEfvs();
        if (!scvMap.isEmpty())
            writeScvs();
        if (!efvMap.isEmpty() || !scvMap.isEmpty()) {
            writeEFSCs();
            writeUVals();
        }

        writeDesignElements();
        writeData(assayDataWriter);

        if (storages.size() != 1)
            canWriteFirstFull = false;

        if (pvalDataMap != null) {
            writeData(pvalDataWriter);
        }
        if (tstatDataMap != null) {
            writeData(tstatDataWriter);
        }
    }

    private interface DataWriterSpec<ColumnType> {
        Iterable<ColumnType> getColumnsForStorage(DataMatrixStorage storage);

        DataMatrixStorage.ColumnRef getColumnRefForColumn(ColumnType column);

        int getDestinationForColumn(ColumnType column);

        String getVariableName();
    }

    private DataWriterSpec<Assay> assayDataWriter = new DataWriterSpec<Assay>() {
        public Iterable<Assay> getColumnsForStorage(DataMatrixStorage storage) {
            return storageAssaysMap.get(storage);
        }

        public DataMatrixStorage.ColumnRef getColumnRefForColumn(Assay assay) {
            return assayDataMap.get(assay.getAccession());
        }

        public int getDestinationForColumn(Assay assay) {
            return assays.indexOf(assay);
        }

        public String getVariableName() {
            return "BDC";
        }
    };

    private abstract class UniqueValueDataWriter implements DataWriterSpec<Pair<String, String>> {
        protected abstract Map<Pair<String, String>, DataMatrixStorage.ColumnRef> getMap();

        public Iterable<Pair<String, String>> getColumnsForStorage(final DataMatrixStorage storage) {

            return new Iterable<Pair<String, String>>() {
                public Iterator<Pair<String, String>> iterator() {
                    return Iterators.filter(
                            new FlattenIterator<String, Pair<String, String>>(efScs.iterator()) {

                                public Iterator<Pair<String, String>> inner(final String property) {
                                    return new MappingIterator<String, Pair<String, String>>(propertyToSortedUniqueValues.get(property).iterator()) {
                                        @Override
                                        public Pair<String, String> map(String value) {
                                            return Pair.create(property, value);
                                        }
                                    };
                                }
                            }, new Predicate<Pair<String, String>>() {
                                public boolean apply(@Nullable Pair<String, String> input) {
                                    return getMap().get(input).storage == storage;
                                }
                            });
                }
            };
        }

        public DataMatrixStorage.ColumnRef getColumnRefForColumn(Pair<String, String> column) {
            return getMap().get(column);
        }

        public int getDestinationForColumn(Pair<String, String> column) {
            int i = 0;
            for (Map.Entry<String, List<String>> entry : propertyToSortedUniqueValues.entrySet()) {
                String property = entry.getKey();
                for (String value : entry.getValue()) {
                    if (column.getFirst().equals(property) && column.getSecond().equals(value))
                        return i;
                    ++i;
                }
            }
            throw new IllegalStateException("Shouldn't be reacheable");
        }
    }

    private DataWriterSpec<Pair<String, String>> pvalDataWriter = new UniqueValueDataWriter() {
        protected Map<Pair<String, String>, DataMatrixStorage.ColumnRef> getMap() {
            return pvalDataMap;
        }

        public String getVariableName() {
            return "PVAL";
        }
    };

    private DataWriterSpec<Pair<String, String>> tstatDataWriter = new UniqueValueDataWriter() {
        protected Map<Pair<String, String>, DataMatrixStorage.ColumnRef> getMap() {
            return tstatDataMap;
        }

        public String getVariableName() {
            return "TSTAT";
        }
    };

    private <ColumnType> void writeData(DataWriterSpec<ColumnType> spec) throws IOException, InvalidRangeException {
        boolean first = true;
        for (DataMatrixStorage storage : storages) {
            if (spec.getColumnsForStorage(storage) == null || !spec.getColumnsForStorage(storage).iterator().hasNext()) // shouldn't happen, but let's be sure
                continue;

            if (first) { // skip first
                first = false;
                if (canWriteFirstFull) {
                    int deNum = 0;
                    for (DataMatrixStorage.Block block : storage.getBlocks()) {
                        writeDataBlock(spec, storage, block, deNum, 0, block.size() - 1);
                        deNum += block.size();
                    }
                    continue;
                }
                // else continue as merging
            }

            // write other buffers finding continuous (in terms of output sequence) blocks of design elements
            for (DataMatrixStorage.Block block : storage.getBlocks()) {
                int startSource = -1;
                int startDestination = -1;
                int currentSource;
                for (currentSource = 0; currentSource < block.size(); ++currentSource) {
                    int currentDestination = mergedDesignElementsMap.get(block.designElements[currentSource]);
                    if (startSource == -1) {
                        startSource = currentSource;
                        startDestination = currentDestination;
                    } else if (currentDestination != startDestination + (currentSource - startSource)) {
                        writeDataBlock(spec, storage, block, startDestination, startSource, currentSource - 1);
                        startSource = currentSource;
                        startDestination = currentDestination;
                    }
                }
                writeDataBlock(spec, storage, block, startDestination, startSource, currentSource - 1);
            }
        }
    }

    private <ColumnType> void writeDataBlock(DataWriterSpec<ColumnType> spec, DataMatrixStorage storage, DataMatrixStorage.Block block, int deNum, int deBlockFrom, int deBlockTo)
            throws IOException, InvalidRangeException {
        int width = storage.getWidth();
        ArrayFloat data = (ArrayFloat) Array.factory(Float.class, new int[]{block.designElements.length, width}, block.expressionValues);

        int startReference = -1;
        int startDestination = -1;
        int currentDestination = -1;
        int currentReference = -1;
        for (ColumnType column : spec.getColumnsForStorage(storage)) {
            int prevReference = currentReference;
            currentReference = spec.getColumnRefForColumn(column).referenceIndex;
            if (currentDestination == -1)
                currentDestination = spec.getDestinationForColumn(column);

            if (startReference == -1) {
                startReference = currentReference;
                startDestination = currentDestination;
            } else if (currentDestination != startDestination + (currentReference - startReference)) {
                ArrayFloat adata = (ArrayFloat) data.sectionNoReduce(
                        Arrays.asList(
                                new Range(deBlockFrom, deBlockTo),
                                new Range(startReference, prevReference)));
                netCdf.write(spec.getVariableName(), new int[]{deNum, startDestination}, adata);
                startReference = currentReference;
                startDestination = currentDestination;
            }

            ++currentDestination;
        }

        ArrayFloat adata = (ArrayFloat) data.sectionNoReduce(
                Arrays.asList(
                        new Range(deBlockFrom, deBlockTo),
                        new Range(startReference, currentReference)));
        netCdf.write(spec.getVariableName(), new int[]{deNum, startDestination}, adata);

    }

    private void writeDesignElements() throws IOException, InvalidRangeException {
        // store design elements one by one
        int i = 0;
        ArrayChar deName = new ArrayChar.D2(1, maxDesignElementLength);
        ArrayInt deIds = new ArrayInt.D1(totalDesignElements);
        ArrayInt gnIds = new ArrayInt.D1(totalDesignElements);
        boolean deMapped = false;
        boolean geneMapped = false;
        for (String de : mergedDesignElements) {
            deName.setString(0, de);
            netCdf.write("DEacc", new int[]{i, 0}, deName);
            Long deId = arrayDesign.getDesignElement(de);
            if (deId != null) {
                deMapped = true;
                deIds.setLong(i, deId);
                List<Long> gnId = arrayDesign.getGeneId(deId);
                // TODO: currently, we only have one gene per DE; we may want to change it later on
                if (gnId != null && !gnId.isEmpty()) {
                    gnIds.setLong(i, gnId.get(0));
                    geneMapped = true;
                }
            }
            ++i;
        }

        netCdf.write("DE", deIds);
        netCdf.write("GN", gnIds);

        if (!deMapped)
            warnings.add("No design element mappings were found");
        if (!geneMapped)
            warnings.add("No gene mappings were found");
    }

    private void writeAssayAccessions() throws IOException, InvalidRangeException {
        writeList("ASacc", assayAccessions);
    }

    private void writeSampleAccessions() throws IOException, InvalidRangeException {
        writeList("BSacc", sampleAccessions);
    }

    private void writeSampleOntologies() throws IOException, InvalidRangeException {
        if ((null != scvOntologies) && (maxScvoLength > 0))
            writeMap("SCVO", scvOntologies, scvOntologies.keySet().size(), samples.size(), maxScvoLength);
    }

    private void writeAssayOntologies() throws IOException, InvalidRangeException {
        if ((null != efvOntologies) && (maxEfvoLength > 0))
            writeMap("EFVO", efvOntologies, efvOntologies.keySet().size(), assays.size(), maxEfvoLength);
    }

    private void writeList(String variable, List<String> values) throws IOException, InvalidRangeException {

        int maxValueLength = 0;
        for (String value : values) {
            if ((null != value) && (value.length() > maxValueLength))
                maxValueLength = value.length();
        }
        ArrayChar valueBuffer = new ArrayChar.D2(1, maxValueLength);
        int i = 0;
        for (String value : values) {
            valueBuffer.setString(0, (null == value ? "" : value));
            netCdf.write(variable, new int[]{i, 0}, valueBuffer);
            ++i;
        }
    }

    //dim1 = map.keySet
    //dim2 = map[0].size
    //dim3 = max length
    private void writeMap(String variable, Multimap<String, String> values, int dim1, int dim2, int dim3) throws IOException, InvalidRangeException {
        int ei;
        ArrayChar valueBuffer = new ArrayChar.D3(dim1, dim2, dim3);
        ei = 0;
        for (Map.Entry<String, Collection<String>> e : values.asMap().entrySet()) {
            int vi = 0;
            for (String v : e.getValue())
                valueBuffer.setString(valueBuffer.getIndex().set(ei, vi++), (null == v ? "" : v));
            ++ei;
        }
        netCdf.write(variable, valueBuffer);
    }

    private void writeSamplesAssays() throws IOException, InvalidRangeException {
        ArrayInt as = new ArrayInt.D1(assays.size());
        IndexIterator asIt = as.getIndexIterator();
        for (Assay assay : assays) {
            asIt.setLongNext(assay.getAssayID());
        }
        netCdf.write("AS", as);

        ArrayInt bs = new ArrayInt.D1(samples.size());
        IndexIterator bsIt = bs.getIndexIterator();
        for (Sample sample : samples) {
            bsIt.setLongNext(sample.getSampleID());
        }
        netCdf.write("BS", bs);

        ArrayInt bs2as = new ArrayInt.D2(samples.size(), assays.size());
        IndexIterator bs2asIt = bs2as.getIndexIterator();

        // iterate over assays and samples,
        for (Sample sample : samples)
            for (Assay assay : assays)
                bs2asIt.setIntNext(samplesMap.containsKey(assay) && samplesMap.get(assay).contains(sample) ? 1 : 0);

        netCdf.write("BS2AS", bs2as);
    }


    /**
     * Write out unique ef-efvs/sc-scvs
     * @throws IOException
     * @throws InvalidRangeException
     */
    private void writeUVals() throws IOException, InvalidRangeException {
        ArrayChar uval = new ArrayChar.D2(totalUniqueValues, maxEfScLength + Math.max(maxEfvLength, maxScvLength) + 2);
        ArrayInt uvalNum = new ArrayInt.D1(propertyToSortedUniqueValues.keySet().size());
        // Now populate unique scvs/efvs
        int ei = 0;
        int uvali = 0;
        for (Map.Entry<String, List<String>> entry : propertyToSortedUniqueValues.entrySet()) {
            List<String> values = entry.getValue();
            uvalNum.setInt(ei, values.size());
            for (String value : values)
                uval.setString(uvali++, entry.getKey() + "||" + value);
            ++ei;
        }
        netCdf.write("uVAL", uval);
        netCdf.write("uVALnum", uvalNum);
    }

    private void writeEfvs() throws IOException, InvalidRangeException {
        // write assay property values
        ArrayChar ef = new ArrayChar.D2(efvMap.keySet().size(), maxEfLength);
        ArrayChar efv = new ArrayChar.D3(efvMap.keySet().size(), assays.size(), maxEfvLength);

        // Populate non-unique efvs
        int ei = 0;
        for (Map.Entry<String, Collection<String>> e : efvMap.asMap().entrySet()) {
            ef.setString(ei, e.getKey());
            int vi = 0;
            for (String v : e.getValue())
                efv.setString(efv.getIndex().set(ei, vi++), v);
            ++ei;
        }

        netCdf.write("EF", ef);
        netCdf.write("EFV", efv);
    }

    private void writeScvs() throws IOException, InvalidRangeException {
        int ei;// sample charactristics
        ArrayChar sc = new ArrayChar.D2(scvMap.keySet().size(), maxScLength);
        ArrayChar scv = new ArrayChar.D3(scvMap.keySet().size(), samples.size(), maxScvLength);

        ei = 0;
        for (Map.Entry<String, Collection<String>> e : scvMap.asMap().entrySet()) {
            sc.setString(ei, e.getKey());
            int vi = 0;
            for (String v : e.getValue())
                scv.setString(scv.getIndex().set(ei, vi++), v);
            ++ei;
        }

        netCdf.write("SC", sc);
        netCdf.write("SCV", scv);
    }

    /**
     * Write out EFSC field - a collection of unique EFs/SCs
     *
     * @throws IOException
     * @throws InvalidRangeException
     */
    private void writeEFSCs() throws IOException, InvalidRangeException {
        int efsci = 0;
        ArrayChar efscAC = new ArrayChar.D2(efScs.size(), maxEfScLength);
        for (String efsc : efScs) {
            efscAC.setString(efsci, efsc);
            efsci++;
        }
        netCdf.write("EFSC", efscAC);
    }

    public void createNetCdf(File file) throws NetCDFCreatorException {
        warnings.clear();
        prepareData();

        try {
            log.info("Writing NetCDF file to " + file);
            netCdf = NetcdfFileWriteable.createNew(file.getAbsolutePath(), true);
            try {
                create();
                write();
            } catch (InvalidRangeException e) {
                throw new NetCDFCreatorException(e);
            } finally {
                netCdf.close();
            }
        } catch (IOException e) {
            throw new NetCDFCreatorException(e);
        }
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean hasWarning() {
        return !warnings.isEmpty();
    }

    private void safeAddGlobalAttribute(String attribute, String value) {
        if ((null != attribute) && (null != value)) {
            netCdf.addGlobalAttribute(
                    attribute,
                    value);
        }
    }

    private void safeAddGlobalAttribute(String attribute, Number value) {
        if ((null != attribute) && (null != value)) {
            netCdf.addGlobalAttribute(
                    attribute,
                    value);
        }
    }

    /**
     * @param efs
     * @param scs
     * @return merged LinkedHashSet of efs and scs keySets
     */
    private LinkedHashSet<String> getEfScs(Multimap<String, String> efs, Multimap<String, String> scs) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        result.addAll(efs.keySet());
        result.addAll(scs.keySet());
        return result;
    }
}
