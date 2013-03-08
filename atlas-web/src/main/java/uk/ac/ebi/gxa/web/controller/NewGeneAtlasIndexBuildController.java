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

package uk.ac.ebi.gxa.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.service.IndexBuilderService;
import uk.ac.ebi.gxa.index.builder.service.NewGeneAtlasIndexBuilderService;

import java.util.concurrent.Executors;

@Controller
public class NewGeneAtlasIndexBuildController {

    @Autowired
    private NewGeneAtlasIndexBuilderService builder;

    private String statusText = "";

    @RequestMapping(value = "/newGeneAtlasIndexBuild/status")
    public String status(final Model model) {
        model.addAttribute("statusText", statusText);
        return "newGeneAtlasIndexBuild/status";
    }

    @RequestMapping(value = "/newGeneAtlasIndexBuild")
    public String build() {

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                builder.build(new IndexAllCommand(), new IndexBuilderService.ProgressUpdater() {
                    public void update(String progress) {
                        statusText = progress;
                    }
                });
                statusText = "Finished building index.";
            }
        });

        return "newGeneAtlasIndexBuild/build";
    }

}