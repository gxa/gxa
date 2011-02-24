package uk.ac.ebi.gxa.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.efo.Efo;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: 2/23/11
 * Time: 4:23 PM
 * Class to represent efo Attributes at bit index query time
 */
public class EfoAttribute extends Attribute {

    private String value;

    final private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Constructor used for efo terms at bit index query time
     * <p/>
     *
     * @param value
     * @param statType
     */
    public EfoAttribute(final String value, final StatisticsType statType) {
        this.value = value.intern();
        this.statType = statType;
    }


    /**
     * @param efo Efo
     * @return Set containing this Attribute and all its children
     */
    @Override
    public Set<Attribute> getAttributeAndChildren(Efo efo) {
        // LinkedHashSet for maintaining order of entry - order of processing attributes may be important
        // in multi-Attribute queries for sorted lists of experiments for the gene page
        Set<Attribute> attrsPlusChildren = new LinkedHashSet<Attribute>();


        Collection<String> efoPlusChildren = efo.getTermAndAllChildrenIds(getValue());
        log.debug("Expanded efo: " + this + " into: " + efoPlusChildren);
        for (String efoTerm : efoPlusChildren) {
            attrsPlusChildren.add(new EfoAttribute(efoTerm, this.getStatType()));
        }
        return attrsPlusChildren;
    }


    /**
     * @param statisticsStorage - used to obtain indexes of attributes and experiments, needed finding experiment counts in bit index
     * @param allExpsToAttrs    Map: Experiment -> Set<Attribute> to which mappings for efo term represented by this Attribute are to be added
     *                          This map groups ef-efv conditions for a given efo term per experiment.
     *                          This is so that when the query is scored, we don't count the experiment multiple times for a given efo term.
     */
    @Override
    public void getEfvExperimentMappings(
            final StatisticsStorage<Long> statisticsStorage,
            Map<Experiment, Set<EfvAttribute>> allExpsToAttrs
    ) {

        Map<Experiment, Set<EfvAttribute>> expsToAttr = statisticsStorage.getMappingsForEfo(getValue());

        if (expsToAttr.isEmpty()) {
            for (Map.Entry<Experiment, Set<EfvAttribute>> expToAttr : expsToAttr.entrySet()) {
                if (!allExpsToAttrs.containsKey(expToAttr.getKey())) {
                    allExpsToAttrs.put(expToAttr.getKey(), new HashSet<EfvAttribute>());
                }

                allExpsToAttrs.get(expToAttr.getKey()).addAll(expToAttr.getValue());
            }
        } else {
            log.debug("No mapping to experiments-efvs was found for efo term: " + getValue());
        }
    }

    @Override
    public String getValue() {
        return value;
    }
}
