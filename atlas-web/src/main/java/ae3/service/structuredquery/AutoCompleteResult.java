package ae3.service.structuredquery;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.*;

/**
 * Class encapsulating results of auto-completion
 *
 * @author rpetry
 * @see uk.ac.ebi.gxa.requesthandlers.helper.FactorValuesRequestHandler
 */
public class AutoCompleteResult {
    private final Multimap<String, AutoCompleteItem> autoCompleteCandidates = Multimaps.newListMultimap(
            Maps.<String, Collection<AutoCompleteItem>>newHashMap(),
            new Supplier<List<AutoCompleteItem>>() {
                @Override
                public List<AutoCompleteItem> get() {
                    return Lists.newArrayList();
                }
            }
    );

    public void add(AutoCompleteItem autoCompleteItem) {
        autoCompleteCandidates.put(autoCompleteItem.getValue(), autoCompleteItem);
    }

    /**
     * @param type type of entity that is being autocompleted. For the list of types that can be passed to this method see atlas-searchform.js.
     * @return Collection of AutoCompleteItem's.
     *         If type="efoefv", for the same efo or efv name this method preferentially chooses the EFO term to offer to the user as an autocomplete option.
     */
    public List<AutoCompleteItem> getResults(String type) {
        List<AutoCompleteItem> res = Lists.newArrayList();
        for (Map.Entry<String, Collection<AutoCompleteItem>> entry : autoCompleteCandidates.asMap().entrySet()) {
            for (AutoCompleteItem item : entry.getValue()) {
                res.add(item);
                if ("efoefv".equals(type))
                    // Store only one autocomplete item per name displayed to the user: autoCompleteItem.getValue())
                    // Note that for type="efoefv" EFO items are populated first (i.e. come earlier in List: entry.getValue()).
                    // Thus EFO terms are picked preferentially over factor values.
                    break;
            }
        }
        return res;
    }
}
