package uk.ac.ebi.gxa.tasks;

import java.util.ArrayList;

// TODO: 4alf: it is a bad idea to inherit from an ArrayList just to add a new field.
// TODO: 4alf: replace with delegation
public class ExperimentList extends ArrayList<ExperimentLine> {
    private int numTotal;

    public int getNumTotal() {
        return numTotal;
    }

    void setNumTotal(int numTotal) {
        this.numTotal = numTotal;
    }
}
