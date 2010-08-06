/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package ae3.anatomogram;

import org.apache.batik.parser.PathHandler;
import org.apache.batik.parser.ParseException;

public class AnnotationPathHandler implements PathHandler {
    private int num_dots;
    private float center_x, center_y, last_x,  last_y;

    public float getCenterX() {
        return center_x;
    }

    public float getCenterY() {
        return center_y;
    }

    public void startPath() throws ParseException {
        //just start
        //throw new ParseException("not implemented", null);
    }

    public void endPath() throws ParseException {
        //just end
        //throw new ParseException("not implemented", null);
    }

    public void movetoRel(float v, float v1) throws ParseException {
        movetoAbs(last_x + v, last_y + v1);
    }

    public void movetoAbs(float v, float v1) throws ParseException {
        float n = (float) num_dots;
        float n_plus_1 = (float) (num_dots + 1);

        last_x = v;
        last_y = v1;

        center_x = center_x * (n / n_plus_1) + (v / n_plus_1);
        center_y = center_y * (n / n_plus_1) + (v1 / n_plus_1);
        ++num_dots;
    }

    public void closePath() throws ParseException {
        //throw new ParseException("not implemented", null);
    }

    public void linetoRel(float v, float v1) throws ParseException {
        movetoRel(v, v1);
        //throw new ParseException("not implemented", null);
    }

    public void linetoAbs(float v, float v1) throws ParseException {
        movetoAbs(v, v1);
        //throw new ParseException("not implemented", null);
    }

    public void linetoHorizontalRel(float v) throws ParseException {
        movetoAbs(last_x + v, last_y);
        //throw new ParseException("not implemented", null);
    }

    public void linetoHorizontalAbs(float v) throws ParseException {
        movetoAbs(v, last_y);
        //throw new ParseException("not implemented", null);
    }

    public void linetoVerticalRel(float v) throws ParseException {
        movetoAbs(last_x, last_y + v);
        //throw new ParseException("not implemented", null);
    }

    public void linetoVerticalAbs(float v) throws ParseException {
        movetoAbs(last_x, v);
        //throw new ParseException("not implemented", null);
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