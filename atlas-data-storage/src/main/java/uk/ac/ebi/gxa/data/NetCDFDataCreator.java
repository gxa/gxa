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

package uk.ac.ebi.gxa.data;

import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.gxa.utils.FlattenIterator;
import uk.ac.ebi.gxa.utils.MappingIterator;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

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
public class NetCDFDataCreator {
    private final Logger log = LoggerFactory.getLogger(NetCDFDataCreator.class);

    private final AtlasDataDAO dataDAO;
    private final Experiment experiment;
    private final ArrayDesign arrayDesign;

    private final List<Assay> assays;
    private final LinkedHashSet<Sample> samples = new LinkedHashSet<Sample>();
    private final ListMultimap<Assay, Sample> samplesMap = ArrayListMultimap.create();

    private Map<String, DataMatrixStorage.ColumnRef> assayDataMap = new HashMap<String, DataMatrixStorage.ColumnRef>();

    private List<DataMatrixStorage> storages = new ArrayList<DataMatrixStorage>();
    private ListMultimap<DataMatrixStorage, Assay> storageAssaysMap = ArrayListMultimap.create();

    private Iterable<String> mergedDesignElements;
    private Map<String, Integer> mergedDesignElementsMap;
    private boolean canWriteFirstFull;

    // maps of properties
    private LinkedHashMap<String, List<String>> efvMap;
    private LinkedHashMap<String, List<String>> scvMap;
    private LinkedHashSet<String> efScs; // efs/scs
    private Multimap<String, String> propertyToUnsortedUniqueValues = LinkedHashMultimap.create(); // sc/ef -> unsorted scvs/efvs
    private Map<String, List<String>> propertyToSortedUniqueValues = new LinkedHashMap<String, List<String>>(); // sc/ef -> sorted scs/efvs

    private List<String> warnings = new ArrayList<String>();

    private int totalDesignElements;
    private int totalUniqueValues; // scvs/efvs
    private int maxDesignElementLength;
    private int maxEfLength;
    private int maxEfScLength;
    private int maxEfvLength;
    private int maxScLength;
    private int maxScvLength;

    NetCDFDataCreator(AtlasDataDAO dataDAO, Experiment experiment, ArrayDesign arrayDesign) {
        this.dataDAO = dataDAO;
        this.experiment = experiment;
        this.arrayDesign = arrayDesign;
        this.assays = new ArrayList<Assay>(experiment.getAssaysForDesign(arrayDesign));
        for (Assay a : this.assays) {
            for (Sample s : a.getSamples()) {
                this.samples.add(s);
                this.samplesMap.put(a, s);
            }
        }
    }

    public void setAssayDataMap(Map<String, DataMatrixStorage.ColumnRef> assayDataMap) {
        this.assayDataMap = assayDataMap;
    }

    private LinkedHashMap<String, List<String>> extractAssayProperties(List<Assay> assays) {
        final LinkedHashMap<String, List<String>> result = new LinkedHashMap<String, List<String>>();

        final SortedSet<String> propertyNames = new TreeSet<String>();

        for (Assay a : assays) {
            propertyNames.addAll(a.getPropertyNames());
        }

        for (String propertyName : propertyNames) {
            final List<String> propertyList = new ArrayList<String>(assays.size());

            for (final Assay a : assays) {
                propertyList.add(a.getPropertySummary(propertyName));
            }

            result.put(propertyName, propertyList);
        }

        return result;
    }

    private LinkedHashMap<String, List<String>> extractSampleProperties(final List<Sample> samples) {
        final LinkedHashMap<String, List<String>> result = new LinkedHashMap<String, List<String>>();

        final SortedSet<String> propertyNames = new TreeSet<String>();

        for (final Sample s : samples) {
            propertyNames.addAll(s.getPropertyNames());
        }

        for (final String propertyName : propertyNames) {
            final List<String> propertyList = new ArrayList<String>(samples.size());

            for (final Sample s : samples) {
                propertyList.add(s.getPropertySummary(propertyName));
            }

            result.put(propertyName, propertyList);
        }

        return result;
    }

    void prepareData() {
        for (Assay a : assays) {
            DataMatrixStorage buf = assayDataMap.get(a.getAccession()).storage;
            if (!storages.contains(buf))
                storages.add(buf);
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
        final List<Sample> samplesList = new ArrayList<Sample>(samples);
        efvMap = extractAssayProperties(assays);
        scvMap = extractSampleProperties(samplesList);

        // Merge efvMap and scvMap into propertyToUnsortedUniqueValues that will store all scv/efv properties
        for (Map.Entry<String, List<String>> efToEfvs : efvMap.entrySet()) {
            propertyToUnsortedUniqueValues.putAll(efToEfvs.getKey(), efToEfvs.getValue());
        }
        for (Map.Entry<String, List<String>> scToScvs : scvMap.entrySet()) {
            propertyToUnsortedUniqueValues.putAll(scToScvs.getKey(), scToScvs.getValue());
        }

        efScs = getEfScs(efvMap, scvMap);

        // find maximum lengths for ef/efv/sc/scv strings
        maxEfLength = 0;
        maxEfScLength = 0;
        maxEfvLength = 0;
        for (String ef : efvMap.keySet()) {
            maxEfLength = Math.max(maxEfLength, ef.length());
            maxEfScLength = Math.max(maxEfScLength, ef.length());
            for (String efv : efvMap.get(ef))
                maxEfvLength = Math.max(maxEfvLength, efv.length());
        }

        maxScLength = 0;
        maxScvLength = 0;
        for (String sc : scvMap.keySet()) {
            maxScLength = Math.max(maxScLength, sc.length());
            maxEfScLength = Math.max(maxEfScLength, sc.length());
            for (String scv : scvMap.get(sc))
                maxScvLength = Math.max(maxScvLength, scv.length());
        }

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
    }

    private void create(NetcdfFileWriteable netCdf) throws IOException {
        // NetCDF doesn't know how to store longs, so we use DataType.DOUBLE for internal DB ids

        final Dimension assayDimension = netCdf.addDimension("AS", assays.size());

        final Dimension sampleDimension = netCdf.addDimension("BS", samples.size());

        netCdf.addVariable(
            "BS2AS", DataType.INT,
            new Dimension[]{sampleDimension, assayDimension}
        );

        // update the netCDFs with the genes count
        final Dimension designElementDimension =
            netCdf.addDimension("DE", totalDesignElements);
        final Dimension designElementLenDimension =
            netCdf.addDimension("DElen", maxDesignElementLength);

        netCdf.addVariable(
            "DEacc", DataType.CHAR,
            new Dimension[]{designElementDimension, designElementLenDimension}
        );
        netCdf.addVariable(
            "GN", DataType.DOUBLE,
            new Dimension[]{designElementDimension}
        );

        //accessions for Assays and Samples
        int maxAssayLength = 0;
        for (Assay assay : assays) {
            maxAssayLength = Math.max(maxAssayLength, assay.getAccession().length());
        }
        final Dimension assayLenDimension = netCdf.addDimension("ASlen", maxAssayLength);
        netCdf.addVariable("ASacc", DataType.CHAR, new Dimension[]{assayDimension, assayLenDimension});

        int maxSampleLength = 0;
        for (Sample sample : samples) {
            maxSampleLength = Math.max(maxSampleLength, sample.getAccession().length());
        }
        final Dimension sampleLenDimension = netCdf.addDimension("BSlen", maxSampleLength);
        netCdf.addVariable("BSacc", DataType.CHAR, new Dimension[]{sampleDimension, sampleLenDimension});

        if (!scvMap.isEmpty() || !efvMap.isEmpty()) {
            if (!scvMap.isEmpty()) {
                Dimension scDimension = netCdf.addDimension("SC", scvMap.keySet().size());
                Dimension sclenDimension = netCdf.addDimension("SClen", maxScLength);

                netCdf.addVariable("SC", DataType.CHAR, new Dimension[]{scDimension, sclenDimension});

                Dimension scvlenDimension = netCdf.addDimension("SCVlen", maxScvLength);
                netCdf.addVariable("SCV", DataType.CHAR, new Dimension[]{scDimension, sampleDimension, scvlenDimension});
            }

            if (!efvMap.isEmpty()) {
                Dimension efDimension = netCdf.addDimension("EF", efvMap.keySet().size());
                Dimension eflenDimension = netCdf.addDimension("EFlen", maxEfLength);

                netCdf.addVariable("EF", DataType.CHAR, new Dimension[]{efDimension, eflenDimension});

                Dimension efvlenDimension = netCdf.addDimension("EFVlen", maxEfLength + maxEfvLength + 2);
                netCdf.addVariable("EFV", DataType.CHAR, new Dimension[]{efDimension, assayDimension, efvlenDimension});
            }
        }

        netCdf.addVariable(
            "BDC", DataType.FLOAT,
            new Dimension[]{designElementDimension, assayDimension}
        );

        // add metadata global attributes
        safeAddGlobalAttribute(
				netCdf,
                "CreateNetCDF_VERSION",
                "2.0");
        safeAddGlobalAttribute(
				netCdf,
                "experiment_accession",
                experiment.getAccession());
        safeAddGlobalAttribute(
				netCdf,
                "ADaccession",
                arrayDesign.getAccession());
		/*
        safeAddGlobalAttribute(
				netCdf,
                "ADid",
                arrayDesign.getArrayDesignID().doubleValue()); // netcdf doesn't know how to store longs
		*/
        safeAddGlobalAttribute(
				netCdf,
                "ADname",
                arrayDesign.getName());
        safeAddGlobalAttribute(
				netCdf,
                "experiment_lab",
                experiment.getLab());
        safeAddGlobalAttribute(
				netCdf,
                "experiment_performer",
                experiment.getPerformer());
        safeAddGlobalAttribute(
				netCdf,
                "experiment_pmid",
                experiment.getPubmedId());
        safeAddGlobalAttribute(
				netCdf,
                "experiment_abstract",
                experiment.getAbstract());

        netCdf.create();
    }

    private void write(NetcdfFileWriteable netCdf) throws IOException, InvalidRangeException {
        writeSamplesAssays(netCdf);
        writeSampleAccessions(netCdf);
        writeAssayAccessions(netCdf);

        if (!efvMap.isEmpty()) {
            writeEfvs(netCdf);
        }
        if (!scvMap.isEmpty()) {
            writeScvs(netCdf);
        }

        writeDesignElements(netCdf);
        writeData(netCdf);

        if (storages.size() != 1) {
            canWriteFirstFull = false;
        }
    }

    private Iterable<Assay> getColumnsForStorage(DataMatrixStorage storage) {
        return storageAssaysMap.get(storage);
    }

    private void writeData(NetcdfFileWriteable netCdf) throws IOException, InvalidRangeException {
        boolean first = true;
        for (DataMatrixStorage storage : storages) {
            if (getColumnsForStorage(storage) == null || !getColumnsForStorage(storage).iterator().hasNext()) // shouldn't happen, but let's be sure
                continue;

            if (first) { // skip first
                first = false;
                if (canWriteFirstFull) {
                    int deNum = 0;
                    for (DataMatrixStorage.Block block : storage.getBlocks()) {
                        writeDataBlock(netCdf, storage, block, deNum, 0, block.size() - 1);
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
                        writeDataBlock(netCdf, storage, block, startDestination, startSource, currentSource - 1);
                        startSource = currentSource;
                        startDestination = currentDestination;
                    }
                }
                writeDataBlock(netCdf, storage, block, startDestination, startSource, currentSource - 1);
            }
        }
    }

    private void writeDataBlock(NetcdfFileWriteable netCdf, DataMatrixStorage storage, DataMatrixStorage.Block block, int deNum, int deBlockFrom, int deBlockTo)
            throws IOException, InvalidRangeException {
        final String variableName = "BDC";

        int width = storage.getWidth();
        ArrayFloat data = (ArrayFloat) Array.factory(Float.class, new int[]{block.designElements.length, width}, block.expressionValues);

        int startReference = -1;
        int startDestination = -1;
        int currentDestination = -1;
        int currentReference = -1;
        for (Assay assay : getColumnsForStorage(storage)) {
            int prevReference = currentReference;
            currentReference = assayDataMap.get(assay.getAccession()).referenceIndex;
            if (currentDestination == -1) {
                currentDestination = assays.indexOf(assay);
            }

            if (startReference == -1) {
                startReference = currentReference;
                startDestination = currentDestination;
            } else if (currentDestination != startDestination + (currentReference - startReference)) {
                ArrayFloat adata = (ArrayFloat) data.sectionNoReduce(
                        Arrays.asList(
                                new Range(deBlockFrom, deBlockTo),
                                new Range(startReference, prevReference)));
                netCdf.write(variableName, new int[]{deNum, startDestination}, adata);
                startReference = currentReference;
                startDestination = currentDestination;
            }

            ++currentDestination;
        }

        ArrayFloat adata = (ArrayFloat) data.sectionNoReduce(
                Arrays.asList(
                        new Range(deBlockFrom, deBlockTo),
                        new Range(startReference, currentReference)));
        netCdf.write(variableName, new int[]{deNum, startDestination}, adata);
    }

    private void writeDesignElements(NetcdfFileWriteable netCdf) throws IOException, InvalidRangeException {
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

        netCdf.write("GN", gnIds);

        if (!deMapped)
            warnings.add("No design element mappings were found");
        if (!geneMapped)
            warnings.add("No gene mappings were found");
    }

    private void writeAssayAccessions(NetcdfFileWriteable netCdf) throws IOException, InvalidRangeException {
        final List<String> accessions = new ArrayList<String>();
        for (Assay a : assays) {
            accessions.add(a.getAccession());
        }
        writeList(netCdf, "ASacc", accessions);
    }

    private void writeSampleAccessions(NetcdfFileWriteable netCdf) throws IOException, InvalidRangeException {
        final List<String> accessions = new ArrayList<String>();
        for (Sample s : samples) {
            accessions.add(s.getAccession());
        }
        writeList(netCdf, "BSacc", accessions);
    }

    private void writeList(NetcdfFileWriteable netCdf, String variable, List<String> values) throws IOException, InvalidRangeException {
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

    private void writeSamplesAssays(NetcdfFileWriteable netCdf) throws IOException, InvalidRangeException {
        ArrayInt bs2as = new ArrayInt.D2(samples.size(), assays.size());
        IndexIterator bs2asIt = bs2as.getIndexIterator();

        // iterate over assays and samples,
        for (Sample sample : samples) {
            for (Assay assay : assays) {
                bs2asIt.setIntNext(samplesMap.containsKey(assay) && samplesMap.get(assay).contains(sample) ? 1 : 0);
            }
        }

        netCdf.write("BS2AS", bs2as);
    }


    private void writeEfvs(NetcdfFileWriteable netCdf) throws IOException, InvalidRangeException {
        // write assay property values
        ArrayChar ef = new ArrayChar.D2(efvMap.keySet().size(), maxEfLength);
        ArrayChar efv = new ArrayChar.D3(efvMap.keySet().size(), assays.size(), maxEfvLength);

        // Populate non-unique efvs
        int ei = 0;
        for (Map.Entry<String, List<String>> e : efvMap.entrySet()) {
            ef.setString(ei, e.getKey());
            int vi = 0;
            for (String v : e.getValue())
                efv.setString(efv.getIndex().set(ei, vi++), v);
            ++ei;
        }

        netCdf.write("EF", ef);
        netCdf.write("EFV", efv);
    }

    private void writeScvs(NetcdfFileWriteable netCdf) throws IOException, InvalidRangeException {
        ArrayChar sc = new ArrayChar.D2(scvMap.keySet().size(), maxScLength);
        ArrayChar scv = new ArrayChar.D3(scvMap.keySet().size(), samples.size(), maxScvLength);

        int ei = 0;
        for (Map.Entry<String, List<String>> e : scvMap.entrySet()) {
            sc.setString(ei, e.getKey());
            int vi = 0;
            for (String v : e.getValue())
                scv.setString(scv.getIndex().set(ei, vi++), v);
            ++ei;
        }

        netCdf.write("SC", sc);
        netCdf.write("SCV", scv);
    }

    public void createNetCdf() throws AtlasDataException {
        warnings.clear();
        prepareData();

        try {
            final File dataFile = dataDAO.getDataFile(experiment, arrayDesign);
            if (!dataFile.getParentFile().exists() && !dataFile.getParentFile().mkdirs()) {
                throw new AtlasDataException("Cannot create folder for the output file" + dataFile);
            }

            final File tempDataFile = File.createTempFile(dataFile.getName(), ".tmp");
            log.info("Writing NetCDF file to " + tempDataFile);
            final NetcdfFileWriteable netCdf = NetcdfFileWriteable.createNew(tempDataFile.getAbsolutePath(), true);
            try {
                create(netCdf);
                write(netCdf);
            } catch (InvalidRangeException e) {
                throw new AtlasDataException(e);
            } finally {
                netCdf.close();
            }
            log.info("Renaming " + tempDataFile + " to " + dataFile);
            if (!tempDataFile.renameTo(dataFile)) {
                throw new AtlasDataException("Can't rename " + tempDataFile + " to " + dataFile);
            }
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean hasWarning() {
        return !warnings.isEmpty();
    }

    private void safeAddGlobalAttribute(NetcdfFileWriteable netCdf, String attribute, String value) {
        if (attribute != null && value != null) {
            netCdf.addGlobalAttribute(attribute, value);
        }
    }

    private void safeAddGlobalAttribute(NetcdfFileWriteable netCdf, String attribute, Number value) {
        // geometer: according NetcdfFileWriteable documentation,
        // Long value cannot be stored in NetCDF
        if (value instanceof Long) {
            safeAddGlobalAttribute(netCdf, attribute, value.toString());
            return;
        }
        if (attribute != null && value != null) {
            netCdf.addGlobalAttribute(attribute, value);
        }
    }

    /**
     * @param efs
     * @param scs
     * @return merged LinkedHashSet of efs and scs keySets
     */
    private LinkedHashSet<String> getEfScs(LinkedHashMap<String, List<String>> efs,
                                           LinkedHashMap<String, List<String>> scs) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        result.addAll(efs.keySet());
        result.addAll(scs.keySet());
        return result;
    }
}
