<?xml version="1.0"?>
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:output method="xml" omit-xml-declaration="yes"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
        <!--


        <?xml version="1.0" encoding="UTF-8"?>
<response>
<lst name="responseHeader">
    <int name="status">0</int>
    <int name="QTime">827</int>
    <lst name="params">
        <str name="wt">xml</str>
        <str name="q">wnt</str>
    </lst>
</lst>
<result name="response" numFound="385" start="0">
    <doc>
        <arr name="gene_embl">
            <str>AK135561</str>
        </arr>
        <arr name="gene_ensgene">
            <str>ENSMUSG00000073745</str>
        </arr>
        <arr name="gene_ensprotein">
            <str>ENSMUSP00000095451</str>
        </arr>
        <arr name="gene_enstranscript">
            <str>ENSMUST00000097840</str>
        </arr>
        <arr name="gene_experiment">
            <str>302184262</str>
            <str>315912305</str>
            <str>294904494</str>
            <str>317071559</str>
            <str>318108116</str>
            <str>318445443</str>
            <str>319441140</str>
            <str>320297826</str>
            <str>325024687</str>
            <str>325295297</str>
            <str>325701228</str>
            <str>325971850</str>
            <str>326088010</str>
            <str>326464053</str>
            <str>327026101</str>
            <str>258381576</str>
            <str>253155730</str>
            <str>174501824</str>
            <str>174953316</str>
            <str>191736105</str>
            <str>186766967</str>
            <str>245079616</str>
            <str>247275389</str>
            <str>248042150</str>
            <str>247771528</str>
            <str>184763873</str>
            <str>250090942</str>
            <str>279613828</str>
            <str>198591539</str>
            <str>242435187</str>
            <str>189760770</str>
            <str>185174498</str>
            <str>253471455</str>
            <str>272574290</str>
        </arr>
        <str name="gene_id">170958972</str>
        <str name="gene_identifier">ENSMUSG00000073745</str>
        <str name="gene_name"/>
        <arr name="gene_species">
            <str>MUS MUSCULUS</str>
        </arr>
        <arr name="gene_synonym">
            <str>Wnt4</str>
        </arr>
        <arr name="gene_uniprot">
            <str>Q3UXI1</str>
        </arr>
        <date name="timestamp">2008-01-09T18:31:36.557Z</date>
    </doc>
    <doc>
        <arr name="gene_ensgene">
            <str>ENSDARG00000004562</str>
        </arr>
        <arr name="gene_experiment">
            <str>295319160</str>
        </arr>
        <str name="gene_id">295456930</str>
        <str name="gene_identifier">ENSDARG00000004562</str>
        <str name="gene_name">dr_wnt16</str>
        <arr name="gene_species">
            <str>DANIO RERIO</str>
        </arr>
        <arr name="gene_synonym">
            <str>wnt16</str>
        </arr>
        <date name="timestamp">2007-11-15T17:38:03.110Z</date>
    </doc>
    <doc>
        <arr name="gene_ensgene">
            <str>ENSDARG00000055526</str>
        </arr>
        <arr name="gene_experiment">
            <str>295319160</str>
        </arr>
        <str name="gene_id">295420435</str>
        <str name="gene_identifier">ENSDARG00000055526</str>
        <str name="gene_name">dr_wnt10b</str>
        <arr name="gene_species">
            <str>DANIO RERIO</str>
        </arr>
        <arr name="gene_synonym">
            <str>wnt10b</str>
        </arr>
        <date name="timestamp">2007-11-15T17:37:54.943Z</date>
    </doc>
    <doc>
        <arr name="gene_experiment">
            <str>295319160</str>
        </arr>
        <str name="gene_id">295420436</str>
        <str name="gene_identifier">ZDB-GENE-001106-1</str>
        <str name="gene_name">dr_wnt3l</str>
        <arr name="gene_species">
            <str>DANIO RERIO</str>
        </arr>
        <arr name="gene_synonym">
            <str>wnt3l</str>
        </arr>
        <date name="timestamp">2007-11-15T17:37:54.949Z</date>
    </doc>
    <doc>
        <arr name="gene_ensgene">
            <str>ENSDARG00000058674</str>
        </arr>
        <arr name="gene_experiment">
            <str>295319160</str>
        </arr>
        <str name="gene_id">295420437</str>
        <str name="gene_identifier">ENSDARG00000058674</str>
        <str name="gene_name">dr_wnt5b</str>
        <arr name="gene_species">
            <str>DANIO RERIO</str>
        </arr>
        <arr name="gene_synonym">
            <str>wnt5b</str>
        </arr>
        <date name="timestamp">2007-11-15T17:37:54.951Z</date>
    </doc>
    <doc>
        <arr name="gene_ensgene">
            <str>ENSDARG00000044827</str>
        </arr>
        <arr name="gene_experiment">
            <str>295319160</str>
        </arr>
        <str name="gene_id">295420438</str>
        <str name="gene_identifier">ENSDARG00000044827</str>
        <str name="gene_name">dr_wnt7a</str>
        <arr name="gene_species">
            <str>DANIO RERIO</str>
        </arr>
        <arr name="gene_synonym">
            <str>wnt7a</str>
        </arr>
        <date name="timestamp">2007-11-15T17:37:54.951Z</date>
    </doc>
    <doc>
        <arr name="gene_embl">
            <str>U10870</str>
        </arr>
        <arr name="gene_ensgene">
            <str>ENSDARG00000006911</str>
        </arr>
        <arr name="gene_ensprotein">
            <str>ENSDARP00000049623</str>
        </arr>
        <arr name="gene_enstranscript">
            <str>ENSDART00000049624</str>
        </arr>
        <arr name="gene_experiment">
            <str>295319160</str>
            <str>222060959</str>
            <str>207360929</str>
            <str>192555769</str>
            <str>192696383</str>
            <str>170970068</str>
            <str>171733282</str>
        </arr>
        <arr name="gene_goid">
            <str>GO:0001654</str>
            <str>GO:0009880</str>
            <str>GO:0016055</str>
            <str>GO:0021854</str>
        </arr>
        <arr name="gene_goterm">
            <str>Wnt receptor signaling pathway</str>
            <str>embryonic pattern specification</str>
            <str>eye development</str>
            <str>hypothalamus development</str>
        </arr>
        <str name="gene_id">170032713</str>
        <str name="gene_identifier">ENSDARG00000006911</str>
        <arr name="gene_interproid">
            <str>IPR005816</str>
            <str>IPR005817</str>
            <str>IPR013301</str>
        </arr>
        <arr name="gene_interproterm">
            <str>Secreted growth factor Wnt protein</str>
            <str>Wnt superfamily</str>
            <str>Wnt-8 protein</str>
        </arr>
        <arr name="gene_keyword">
            <str>Developmental protein</str>
            <str>Extracellular matrix</str>
            <str>Glycoprotein</str>
            <str>Signal</str>
            <str>Wnt signaling pathway</str>
            <str>Secreted</str>
        </arr>
        <str name="gene_name">wnt8b</str>
        <arr name="gene_refseq">
            <str>NM_130959</str>
        </arr>
        <arr name="gene_species">
            <str>DANIO RERIO</str>
        </arr>
        <arr name="gene_synonym">
            <str>wnt-8b</str>
            <str>wnt8b</str>
        </arr>
        <arr name="gene_unigene">
            <str>Dr.623</str>
        </arr>
        <arr name="gene_uniprot">
            <str>P51029</str>
        </arr>
        <date name="timestamp">2007-11-15T17:37:54.953Z</date>
    </doc>
    <doc>
        <arr name="gene_experiment">
            <str>295319160</str>
        </arr>
        <str name="gene_id">295510304</str>
        <str name="gene_identifier">ZDB-GENE-060824-6</str>
        <str name="gene_name">dr_wnt2bb</str>
        <arr name="gene_species">
            <str>DANIO RERIO</str>
        </arr>
        <arr name="gene_synonym">
            <str>wnt2bb</str>
        </arr>
        <date name="timestamp">2007-11-15T17:38:06.277Z</date>
    </doc>
    <doc>
        <arr name="gene_embl">
            <str>AF249266</str>
            <str>BC066498</str>
        </arr>
        <arr name="gene_ensgene">
            <str>ENSDARG00000014796</str>
        </arr>
        <arr name="gene_ensprotein">
            <str>ENSDARP00000011998</str>
            <str>ENSDARP00000090653</str>
        </arr>
        <arr name="gene_enstranscript">
            <str>ENSDART00000010909</str>
            <str>ENSDART00000099880</str>
        </arr>
        <arr name="gene_experiment">
            <str>295319160</str>
            <str>222060959</str>
            <str>207360929</str>
            <str>192555769</str>
            <str>192696383</str>
            <str>170970068</str>
            <str>171733282</str>
        </arr>
        <arr name="gene_goid">
            <str>GO:0007507</str>
            <str>GO:0042074</str>
        </arr>
        <arr name="gene_goterm">
            <str>cell migration involved in gastrulation</str>
            <str>heart development</str>
        </arr>
        <str name="gene_id">220876050</str>
        <str name="gene_identifier">ENSDARG00000014796</str>
        <arr name="gene_interproid">
            <str>IPR005816</str>
            <str>IPR005817</str>
        </arr>
        <arr name="gene_interproterm">
            <str>Secreted growth factor Wnt protein</str>
            <str>Wnt superfamily</str>
        </arr>
        <arr name="gene_keyword">
            <str>Developmental protein</str>
            <str>Extracellular matrix</str>
            <str>Secreted</str>
            <str>Wnt signaling pathway</str>
        </arr>
        <str name="gene_name">wnt11r</str>
        <arr name="gene_refseq">
            <str>NM_131076</str>
        </arr>
        <arr name="gene_species">
            <str>DANIO RERIO</str>
        </arr>
        <arr name="gene_synonym">
            <str>wnt11r</str>
        </arr>
        <arr name="gene_unigene">
            <str>Dr.107535</str>
            <str>Dr.129086</str>
            <str>Dr.140393</str>
            <str>Dr.76466</str>
        </arr>
        <arr name="gene_uniprot">
            <str>Q9I9I2</str>
        </arr>
        <date name="timestamp">2007-11-15T17:37:54.946Z</date>
    </doc>
    <doc>
        <arr name="gene_embl">
            <str>AB060966</str>
            <str>AY009399</str>
            <str>BC001749</str>
        </arr>
        <arr name="gene_ensgene">
            <str>ENSG00000111186</str>
        </arr>
        <arr name="gene_ensprotein">
            <str>ENSP00000308887</str>
            <str>ENSP00000380379</str>
        </arr>
        <arr name="gene_enstranscript">
            <str>ENST00000310594</str>
            <str>ENST00000397196</str>
        </arr>
        <arr name="gene_experiment">
            <str>226010852</str>
            <str>175818773</str>
            <str>281616966</str>
            <str>281838716</str>
            <str>281899205</str>
            <str>283209670</str>
            <str>283362751</str>
            <str>280939376</str>
            <str>194286559</str>
            <str>297079008</str>
            <str>275865806</str>
            <str>194298445</str>
            <str>304587998</str>
            <str>304694355</str>
            <str>305920379</str>
            <str>306315669</str>
            <str>311117466</str>
            <str>333734866</str>
            <str>307715628</str>
            <str>311176991</str>
            <str>313261537</str>
            <str>313443916</str>
            <str>313671463</str>
            <str>197662023</str>
            <str>313770467</str>
            <str>290453121</str>
            <str>328867738</str>
            <str>290970595</str>
            <str>329653982</str>
            <str>297781125</str>
            <str>272724918</str>
            <str>277916095</str>
            <str>296338739</str>
            <str>332166791</str>
            <str>297346433</str>
            <str>209176591</str>
            <str>275931815</str>
            <str>295514122</str>
            <str>236405289</str>
            <str>153638622</str>
            <str>162713614</str>
            <str>219977237</str>
            <str>223043555</str>
            <str>154329461</str>
            <str>155131725</str>
            <str>160745234</str>
            <str>171885041</str>
            <str>155310011</str>
            <str>156312887</str>
            <str>203759232</str>
            <str>291460484</str>
            <str>249664758</str>
            <str>160657270</str>
            <str>157115151</str>
            <str>204778371</str>
            <str>157560856</str>
            <str>205009025</str>
            <str>291562435</str>
            <str>203223271</str>
            <str>334420642</str>
            <str>190750326</str>
            <str>158028845</str>
            <str>199674015</str>
            <str>169013544</str>
            <str>162385547</str>
            <str>153103778</str>
            <str>158563690</str>
            <str>158875684</str>
            <str>162766389</str>
            <str>208684494</str>
            <str>258922816</str>
            <str>248312774</str>
            <str>241333605</str>
            <str>334420710</str>
            <str>256278422</str>
            <str>215315583</str>
            <str>238274491</str>
            <str>211794549</str>
            <str>251679648</str>
            <str>166452416</str>
            <str>257668644</str>
            <str>164406703</str>
            <str>165577581</str>
            <str>200794190</str>
        </arr>
        <str name="gene_id">153073159</str>
        <str name="gene_identifier">ENSG00000111186</str>
        <arr name="gene_interproid">
            <str>IPR005816</str>
            <str>IPR005817</str>
        </arr>
        <arr name="gene_interproterm">
            <str>Secreted growth factor Wnt protein</str>
            <str>Wnt superfamily</str>
        </arr>
        <arr name="gene_keyword">
            <str>Developmental protein</str>
            <str>Extracellular matrix</str>
            <str>Glycoprotein</str>
            <str>Signal</str>
            <str>Wnt signaling pathway</str>
            <str>Secreted</str>
        </arr>
        <str name="gene_name">WNT5B</str>
        <arr name="gene_refseq">
            <str>NM_030775</str>
            <str>NM_032642</str>
        </arr>
        <arr name="gene_species">
            <str>HOMO SAPIENS</str>
        </arr>
        <arr name="gene_synonym">
            <str>WNT5B</str>
        </arr>
        <arr name="gene_unigene">
            <str>Hs.306051</str>
        </arr>
        <arr name="gene_uniprot">
            <str>Q9H1J7</str>
        </arr>
        <date name="timestamp">2008-01-18T15:30:26.653Z</date>
    </doc>
</result>
</response>


        <?xml version="1.0" encoding="UTF-8"?>
<response>
<lst name="responseHeader">
    <int name="status">0</int>
    <int name="QTime">1</int>
    <lst name="params">
        <str name="wt">xml</str>
        <str name="q">wnt</str>
    </lst>
</lst>
<result name="response" numFound="1" start="0">
    <doc>
        <arr name="ba_devstage">
            <str>E14.5</str>
            <str>E16.5</str>
        </arr>
        <arr name="ba_genotype">
            <str>beta-catenin null</str>
            <str>wild_type</str>
        </arr>
        <arr name="bs_attribute">
            <str>pancreas</str>
            <str>wild_type</str>
            <str>E14.5</str>
            <str>beta-catenin null</str>
            <str>E16.5</str>
        </arr>
        <str name="exp_accession">E-GEOD-7430</str>
        <str name="exp_description">Transcription profiling of mouse pancreas at embryonic days 14.5 and 16.5 from
            animals lacking beta-catenin, a mediator of Wnt signaling and a component of the cadherin-catenin epithelial
            adhesion complex
        </str>
        <arr name="exp_factors">
            <str>E14.5</str>
            <str>E16.5</str>
        </arr>
        <str name="exp_id">326464053</str>
        <arr name="exp_type">
            <str>individual_genetic_characteristics_design</str>
        </arr>
        <int name="popularity">0</int>
        <date name="timestamp">2008-01-09T18:17:52.521Z</date>
    </doc>
</result>
</response>
-->