package ae3.model;

import ae3.service.AtlasStatisticsQueryService;
import ae3.service.structuredquery.UpdownCounter;
import uk.ac.ebi.gxa.utils.EfvTree;

import java.util.*;

public class ExperimentalFactor {
    public int RESULT_ALL_VALUES_SIZE = 6;
    private AtlasGene gene;
    private Collection<String> omittedEfs;
    private String name;
    private HashMap<Long, String> experimentAccessions;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;

    public ExperimentalFactor(AtlasGene gene, String name, Collection<String> omittedEfs, AtlasStatisticsQueryService atlasStatisticsQueryService) {
        this.name = name;
        this.gene = gene;
        this.omittedEfs = omittedEfs;
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
    }

    public String getName() {
        return this.name;
    }

    public List<EfvTree.EfEfv<UpdownCounter>> getValues() {
        List<EfvTree.EfEfv<UpdownCounter>> result = new ArrayList<EfvTree.EfEfv<UpdownCounter>>();

        for (EfvTree.EfEfv<UpdownCounter> f : gene.getHeatMap(this.name, omittedEfs, atlasStatisticsQueryService).getNameSortedList()) {
            if (f.getEf().equals(this.name)) {
                result.add(f);
            }
        }

        return result;
    }

    public List<EfvTree.EfEfv<UpdownCounter>> getTopValues() {
        List<EfvTree.EfEfv<UpdownCounter>> result = new ArrayList<EfvTree.EfEfv<UpdownCounter>>();

        for (EfvTree.EfEfv<UpdownCounter> f : gene.getHeatMap(this.name, omittedEfs, atlasStatisticsQueryService).getNameSortedList()) {
            if (f.getEf().equals(this.name)) {
                if (result.size() < RESULT_ALL_VALUES_SIZE) {
                    result.add(f);
                }
            }
        }
        return result;
    }

    public int getMoreValuesCount() {
        //store overspill count (if slow).
        return getValues().size() - getTopValues().size();
    }

    public Collection<String> getExperiments() {
        return experimentAccessions.values();
    }

    public void addExperiment(Long id, String Accession) {
        if (experimentAccessions == null)
            experimentAccessions = new HashMap<Long, String>();

        experimentAccessions.put(id, Accession);
    }

    public Map<Long, String> getExperimentAccessions() {
        return experimentAccessions;
    }
}
