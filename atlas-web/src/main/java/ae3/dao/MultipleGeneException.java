package ae3.dao;

public class MultipleGeneException extends Exception {
	public MultipleGeneException(String gene_identifier) {
		super("Gene "+ gene_identifier+" hits more than one gene. Should have redirected to heatmap view.");
	}
}
