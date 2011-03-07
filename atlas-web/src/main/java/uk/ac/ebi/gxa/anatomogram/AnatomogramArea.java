package uk.ac.ebi.gxa.anatomogram;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Olga Melnichuk
 *         Date: 28/02/2011
 */
public class AnatomogramArea {

    private String efo;
    private List<Float> coordinates = new ArrayList<Float>();

    public AnatomogramArea(String efo, Collection<Point2D.Float> points) {
        this.efo = efo;
        for (Point2D.Float point : points) {
            coordinates.add(point.x);
            coordinates.add(point.y);
        }
    }

    public String getEfo() {
        return efo;
    }

    public Collection<Float> getCoordinates() {
        return coordinates;
    }
}
