package uk.ac.ebi.gxa.web.controller;

import com.google.common.base.Function;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.statistics.EfAttribute;
import uk.ac.ebi.gxa.statistics.ExperimentResult;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Property;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

import static com.google.common.collect.Collections2.transform;
import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;

@JsonSerialize
public class GenePageExperiment {
    private static final Logger log = LoggerFactory.getLogger(GenePageExperiment.class);

    private Experiment experiment;
    private ExperimentResult experimentInfo;

    public GenePageExperiment(Experiment experiment, ExperimentResult experimentInfo) {
        this.experiment = experiment;
        this.experimentInfo = experimentInfo;
    }

    public EfAttribute getHighestRankAttribute() {
        EfAttribute attribute = experimentInfo.getHighestRankAttribute();
        if (attribute == null || attribute.getEf() == null) {
            log.error("Failed to find highest rank attribute in: " + experimentInfo);
        }
        return attribute;
    }

    public String getAccession() {
        return experiment.getAccession();
    }

    public long getId() {
        return experiment.getId();
    }

    public String getDescription() {
        return experiment.getDescription();
    }

    public String getPubmedId() {
        return experiment.getPubmedId();
    }

    /**
     * Returns set of experiment factors
     *
     * @return all factors from the experiment
     */
    public Collection<Map<String, String>> getExperimentFactors() {
        return transform(experiment.getFactors(),
                new Function<Property, Map<String, String>>() {
                    @Override
                    public Map<String, String> apply(@Nullable Property input) {
                        if (input == null)
                            throw LogUtil.createUnexpected("Null property in an experiment " + experiment);

                        return makeMap(
                                "name", input.getName(),
                                "displayName", input.getDisplayName());
                    }
                });
    }
}
