package ae3.service.structuredquery;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Oct 11, 2010
 * Time: 9:18:35 AM
 * To change this template use File | Settings | File Templates.
 *
 * Find Genes For Experiment Query - result set sort order
 */
public enum QueryResultSortOrder {
    GENE,     // By gene name (in ASC alphabetical order)
    DE,       // By design element Id (ASC)
    COND,     // By ef-efv (in ASC alphabetical order)
    UD,       // In the following order: UP/DOWN, UP, DOWN, NON_D_E
    PVALUE,   // By pval (ASC - best pvals first)
    TSTAT     // By tstat (DESC - best tstats first)
}
