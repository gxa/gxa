package uk.ac.ebi.arrayexpress2.magetab.lang;

/**
 * An abstract implementation of {@link uk.ac.ebi.arrayexpress2.magetab.lang.Progressible}.
 * This implements all the functionality required to get and update the progress
 * for any concrete implementations of this class, rounding down to 8 decimal
 * places.
 *
 * @author Tony Burdett
 */
public abstract class AbstractProgressible implements Progressible {
    private double complete = 0;
    private static final double ACCURACY = 1e8;

    public synchronized int getProgress() {
        return (int) Math.floor(Math.round((complete * ACCURACY)) / ACCURACY);
    }

    public synchronized void increaseProgressBy(double increase) {
        complete += increase;
        notifyAll();
    }
}
