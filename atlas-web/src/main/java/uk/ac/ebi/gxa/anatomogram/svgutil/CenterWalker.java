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

package uk.ac.ebi.gxa.anatomogram.svgutil;

import java.awt.geom.Point2D;

class CenterWalker extends PathWalker {
    private int num_dots;
    private float center_x, center_y;

    public Point2D.Float getCenter() {
        return new Point2D.Float(center_x, center_y);
    }

    protected void visit(float x, float y) {
        final float n = (float) num_dots;
        center_x = (center_x * n + x) / (n + 1f);
        center_y = (center_y * n + y) / (n + 1f);
        num_dots++;
    }
}