package uk.ac.ebi.gxa.anatomogram.svgutil;

import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PathHandler;

public abstract class PathWalker implements PathHandler {
    private float last_x;
    private float last_y;

    public void startPath() throws ParseException {
    }

    public void endPath() throws ParseException {
    }

    public void movetoRel(float v, float v1) throws ParseException {
        movetoAbs(last_x + v, last_y + v1);
    }

    public void movetoAbs(float v, float v1) throws ParseException {
        last_x = v;
        last_y = v1;

        visit(v, v1);
    }

    protected abstract void visit(float x, float y);

    public void closePath() throws ParseException {
    }

    public void linetoRel(float v, float v1) throws ParseException {
        movetoRel(v, v1);
    }

    public void linetoAbs(float v, float v1) throws ParseException {
        movetoAbs(v, v1);
    }

    public void linetoHorizontalRel(float v) throws ParseException {
        movetoAbs(last_x + v, last_y);
    }

    public void linetoHorizontalAbs(float v) throws ParseException {
        movetoAbs(v, last_y);
    }

    public void linetoVerticalRel(float v) throws ParseException {
        movetoAbs(last_x, last_y + v);
    }

    public void linetoVerticalAbs(float v) throws ParseException {
        movetoAbs(last_x, v);
    }

    public void curvetoCubicRel(float v, float v1, float v2, float v3, float v4, float v5) throws ParseException {
        float last_x_local = last_x;
        float last_y_local = last_y;

        movetoAbs(last_x_local + v, last_y_local + v1);
        movetoAbs(last_x_local + v2, last_y_local + v3);
        movetoAbs(last_x_local + v4, last_y_local + v5);
    }

    public void curvetoCubicAbs(float v, float v1, float v2, float v3, float v4, float v5) throws ParseException {
        movetoAbs(v, v1);
        movetoAbs(v2, v3);
        movetoAbs(v4, v5);
    }

    public void curvetoCubicSmoothRel(float v, float v1, float v2, float v3) throws ParseException {
        float last_x_local = last_x;
        float last_y_local = last_y;

        movetoAbs(last_x_local + v, last_y_local + v1);
        movetoAbs(last_x_local + v2, last_y_local + v3);
    }

    public void curvetoCubicSmoothAbs(float v, float v1, float v2, float v3) throws ParseException {
        movetoAbs(v, v1);
        movetoAbs(v2, v3);
    }

    public void curvetoQuadraticRel(float v, float v1, float v2, float v3) throws ParseException {
        float last_x_local = last_x;
        float last_y_local = last_y;

        movetoAbs(last_x_local + v, last_y_local + v1);
        movetoAbs(last_x_local + v2, last_y_local + v3);
    }

    public void curvetoQuadraticAbs(float v, float v1, float v2, float v3) throws ParseException {
        movetoAbs(v, v1);
        movetoAbs(v2, v3);
    }

    public void curvetoQuadraticSmoothRel(float v, float v1) throws ParseException {
        movetoAbs(last_x + v, last_y + v1);
    }

    public void curvetoQuadraticSmoothAbs(float v, float v1) throws ParseException {
        movetoAbs(v, v1);
    }

    public void arcRel(float v, float v1, float v2, boolean b, boolean b1, float v3, float v4) throws ParseException {
        float last_x_local = last_x;
        float last_y_local = last_y;

        movetoAbs(last_x_local + v, last_y_local + v1);
        movetoAbs(last_x_local + v2, last_y_local + v3);
    }

    public void arcAbs(float v, float v1, float v2, boolean b, boolean b1, float v3, float v4) throws ParseException {
        movetoAbs(v, v1);
        movetoAbs(v2, v3);
    }
}
