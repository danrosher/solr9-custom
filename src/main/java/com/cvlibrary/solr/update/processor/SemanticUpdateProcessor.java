package com.cvlibrary.solr.update.processor;

import com.cvlibrary.solr.util.SemanticUtil;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SemanticUpdateProcessor extends UpdateRequestProcessorFactory {
    private static final String VECTOR_MAP = "vector_map";
    private static final String FIELD = "toField";
    private static final String FROM = "fromField";

    private static final String URI = "uri";


    protected static Map<String, NamedList<String>> vector_map = new ConcurrentHashMap<>();


    @Override
    public void init(NamedList<?> args) {
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                if (args.getName(i).equals(VECTOR_MAP)) {
                    NamedList<String> vecMapParams = (NamedList<String>) args.getVal(i);
                    String field = vecMapParams.get(FIELD);
                    if (field == null || "".equals(field)) {
                        throw new RuntimeException("More than one dictionary is missing name.");
                    }
                    vector_map.put(field, vecMapParams);
                }
            }
        }
    }


    @Override
    public UpdateRequestProcessor getInstance(SolrQueryRequest req, SolrQueryResponse rsp, UpdateRequestProcessor next) {
        return new SemanticProcessor(next);
    }

    static class SemanticProcessor extends UpdateRequestProcessor {

        public SemanticProcessor(UpdateRequestProcessor next) {
            super(next);
        }

        @Override
        public void processAdd(AddUpdateCommand cmd) throws IOException {
            SolrInputDocument doc = cmd.getSolrInputDocument();
            for (Map.Entry<String, NamedList<String>> e : vector_map.entrySet()) {
                String f = e.getKey();
                NamedList<String> params = e.getValue();
                String copyField = params.get(FROM);
                if ( doc.containsKey(copyField)) {
                    doc.setField(f,
                            SemanticUtil.fetchVectorStringList((String) doc.getFieldValue(copyField),
                                    params.get(URI)));
                }
            }
            super.processAdd(cmd);
        }
    }
}
