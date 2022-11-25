package com.cvlibrary.solr.update.processor;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.index.LogDocMergePolicyFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class TestSemanticUpdateProcessor extends SolrTestCaseJ4 {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @BeforeClass
    public static void beforeTests() throws Exception {
        // we need DVs on point fields to compute stats & facets
        if (Boolean.getBoolean(NUMERIC_POINTS_SYSPROP))
            System.setProperty(NUMERIC_DOCVALUES_SYSPROP, "true");

        // force LogDocMergePolicy so that we get a predictable doc order
        // when doing unsorted group collection
        systemSetPropertySolrTestsMergePolicyFactory(LogDocMergePolicyFactory.class.getName());

        System.setProperty("enable.update.log", "false");
        initCore("solrconfig.xml", "schema.xml");
    }


    @Before
    public void cleanIndex() {
        assertU(delQ("*:*"));
        assertU(commit());
    }

    @AfterClass
    public static void afterTests() {
        systemClearPropertySolrTestsMergePolicyFactory();
    }


    @Test
    public void testSemanticUpdateProcessor() throws Exception {
        clearIndex();

        assertU(adoc("id", "1", "title_vector", "Hello world!"));
        assertU(adoc("id", "2"));
        assertU(commit());

        assertJQ(
                req("q", "*:*"),
                "/response/numFound==2",
                "/response/docs/[0]/id=='1'",
                "/response/docs/[0]/title_vector/[0]==-0.034454998",
                "/response/docs/[1]/id=='2'"
                );
    }

}
