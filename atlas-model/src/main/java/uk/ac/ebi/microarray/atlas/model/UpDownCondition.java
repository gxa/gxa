/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.microarray.atlas.model;

/**
 * @author Olga Melnichuk
 */
public enum UpDownCondition {
    CONDITION_UP("up") {
        @Override
        public boolean apply(UpDownExpression upDown) {
            return upDown.isUp();
        }
    },
    CONDITION_DOWN("down") {
        @Override
        public boolean apply(UpDownExpression upDown) {
            return upDown.isDown();
        }
    },
    CONDITION_NONDE("non-d.e") {
        @Override
        public boolean apply(UpDownExpression upDown) {
            return upDown.isNonDe();
        }
    },
    CONDITION_UP_OR_DOWN("up/down") {
        @Override
        public boolean apply(UpDownExpression upDown) {
            return upDown.isUp() || upDown.isDown();
        }
    },
    CONDITION_ANY("any") {
        @Override
        public boolean apply(UpDownExpression upDown) {
            return true;
        }
    };

    private final String name;

    private UpDownCondition(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract boolean apply(UpDownExpression expression);

}
