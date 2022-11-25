package com.cvlibrary.solr.queryparser;

import com.cvlibrary.solr.util.SemanticUtil;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.DenseVectorField;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.QueryParsing;
import org.noggit.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.cvlibrary.solr.util.SemanticUtil.CONNECT_TIMEOUT;
import static com.cvlibrary.solr.util.SemanticUtil.READ_TIMEOUT;


public class SemanticQParserPlugin extends QParserPlugin {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        return new SemanticQParser(qstr, localParams, params, req);
    }

    static class SemanticQParser extends QParser {

        static final String TOP_K = "topK";
        static final int DEFAULT_TOP_K = 10;

        /**
         * Constructor for the QParser
         *
         * @param qstr        The part of the query string specific to this parser
         * @param localParams The set of parameters that are specific to this QParser. See
         *                    <a href="https://solr.apache.org/guide/local-parameters-in-queries.html">...</a>
         * @param params      The rest of the {@link SolrParams}
         * @param req         The original {@link SolrQueryRequest}.
         */
        public SemanticQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
            super(qstr, localParams, params, req);
        }

        @Override
        public Query parse() {
            String denseVectorField = localParams.get(QueryParsing.F);
            String searchString = localParams.get(QueryParsing.V);
            int topK = localParams.getInt(TOP_K, DEFAULT_TOP_K);
            String uri = localParams.get("u");
            if (uri == null || uri.isEmpty()) {
                throw new SolrException(
                        SolrException.ErrorCode.BAD_REQUEST, "the uri 'u' is missing");
            }

            if (denseVectorField == null || denseVectorField.isEmpty()) {
                throw new SolrException(
                        SolrException.ErrorCode.BAD_REQUEST, "the Dense Vector field 'f' is missing");
            }

            if (searchString == null || searchString.isEmpty()) {
                throw new SolrException(
                        SolrException.ErrorCode.BAD_REQUEST, "the Dense Vector value 'v' to search is missing");
            }

            SchemaField schemaField = req.getCore().getLatestSchema().getField(denseVectorField);
            FieldType fieldType = schemaField.getType();
            if (!(fieldType instanceof DenseVectorField denseVectorType)) {
                throw new SolrException(
                        SolrException.ErrorCode.BAD_REQUEST,
                        "only DenseVectorField is compatible with Knn Query Parser");
            }

            float[] vectorToSearch;
            try {
                int connectTimeout = localParams.getInt("cTimeout", CONNECT_TIMEOUT);
                int readTimeout = localParams.getInt("rTimeout", READ_TIMEOUT);
                vectorToSearch = SemanticUtil.fetchVectorFloatArray(searchString, uri,connectTimeout,readTimeout);
            } catch (IOException | JSONParser.ParseException e) {
                //skip and carry on with constant score 1
                log.error("Error fetching from semantic service:" + e.getLocalizedMessage());
                return new ConstantScoreQuery(new MatchAllDocsQuery());
            }
            return denseVectorType.getKnnVectorQuery(schemaField.getName(), vectorToSearch, topK);
        }

    }
}
