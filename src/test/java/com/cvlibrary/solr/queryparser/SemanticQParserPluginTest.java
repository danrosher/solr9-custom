package com.cvlibrary.solr.queryparser;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.index.LogDocMergePolicyFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class SemanticQParserPluginTest extends SolrTestCaseJ4 {

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
  public void semanticQParserPluginTest() throws Exception {
    clearIndex();
    int i = 0;
    for (; i < 2; i++) {
      assertU(adoc("id", String.valueOf(i), "title", "java developer"));
    }
    assertU(adoc("id", "31", "title", "java developer"));
    assertU(commit());

    assertJQ(
            req(
                    "q","java developer",
                    "defType","edismax",
                    "qf","title",
                    "fl","id,title,score"),
            "/response/numFound==3");

    //cannot find id 31 as no title_vector
    assertJQ(
        req(
            "q","{!ss f=title_vector u='http://0.0.0.0/symmetric_embedding'}java",
            "fl","id,title,score"),
        "/response/numFound==3",
            "/response/docs/[0]/id=='0'",
            "/response/docs/[0]/score==0.896899",
            "/response/docs/[1]/id=='1'",
            "/response/docs/[1]/score==0.896899"
    );

    assertJQ(
            req(
                    "q","java",
                    "defType","edismax",
                    "qf","title",
                    "rq","{!rerank reRankQuery=$rqq reRankDocs=4 reRankWeight=1}",
                    "rqq","{!ss f=title_vector u='http://0.0.0.0/symmetric_embedding'}java",
                    "fl","id,title,score"),
            "/response/numFound==3",
            "/response/docs/[0]/id=='0'",
            "/response/docs/[0]/score==0.95759505",
            "/response/docs/[1]/id=='1'",
            "/response/docs/[1]/score==0.95759505"
    );

    //if unable to fetch vector ... carry on
    assertJQ(
            req(
                    "q","java",
                    "defType","edismax",
                    "qf","title",
                    "rq","{!rerank reRankQuery=$rqq reRankDocs=4 reRankWeight=1}",
                    "rqq","{!ss f=title_vector u='http://10.0.0.1/symmetric_embedding'}java",
                    "fl","id,title,score"),
            "/response/numFound==3",
            "/response/docs/[0]/id=='0'",
            "/response/docs/[0]/score==1.0606961",
            "/response/docs/[1]/id=='1'",
            "/response/docs/[1]/score==1.0606961"
    );

  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }
}
