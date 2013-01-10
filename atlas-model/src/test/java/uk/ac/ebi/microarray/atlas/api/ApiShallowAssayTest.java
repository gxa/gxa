/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.microarray.atlas.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
public class ApiShallowAssayTest {

    public static final String ASSAY_ACCESSION = "ASSAY_ACCESSION";
    public static final String DESIGN_ACCESSION = "DESIGN_ACCESSION";
    public static final String DESIGN_NAME = "DESIGN_NAME";
    public static final String DESIGN_PROVIDER = "DESIGN_PROVIDER";
    public static final String DESIGN_TYPE = "DESIGN_TYPE";

    @Mock
    private ArrayDesign design;

    @Mock
    private Assay assay;

    private List<AssayProperty> properties = newArrayList();

    private ApiShallowAssay subject;

    @Before
    public void initializeDesign() throws Exception {

        when(design.getAccession()).thenReturn(DESIGN_ACCESSION);
        when(design.getName()).thenReturn(DESIGN_NAME);
        when(design.getProvider()).thenReturn(DESIGN_PROVIDER);
        when(design.getType()).thenReturn(DESIGN_TYPE);

    }

    @Before
    public void initializeAssay() throws Exception {

        when(assay.getAccession()).thenReturn(ASSAY_ACCESSION);
        when(assay.getArrayDesign()).thenReturn(design);
        when(assay.getProperties()).thenReturn(properties);

    }

    @Test
    public void apiShallowAssayConstructorTest() {
        //given
        subject = new ApiShallowAssay(assay);

        //then
        assertThat(subject.getAccession(), notNullValue());
        assertThat(subject.getArrayDesign(), notNullValue());
        assertThat(subject.getProperties(), notNullValue());

    }

    @Test
    public void apiShallowAssayPropertiesTest() {
        //given
        subject = new ApiShallowAssay(assay);

        //then
        assertThat(subject.getAccession(), is(ASSAY_ACCESSION));
        assertThat(subject.getProperties().size(), is(0));

    }

    @Test
    public void apiShallowAssayDesignTest() {
        //given
        subject = new ApiShallowAssay(assay);

        //then
        assertThat(subject.getAccession(), is(ASSAY_ACCESSION));
        assertThat(subject.getArrayDesign().getAccession(), is(DESIGN_ACCESSION));
        assertThat(subject.getArrayDesign().getName(), is(DESIGN_NAME));
        assertThat(subject.getArrayDesign().getProvider(), is(DESIGN_PROVIDER));
        assertThat(subject.getArrayDesign().getType(), is(DESIGN_TYPE));

    }
}