package com.cvlibrary.solr.util;

import org.apache.solr.common.util.Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonMap;

public class SemanticUtil {

    public static int CONNECT_TIMEOUT = 10;
    public static int READ_TIMEOUT = 50;

    public static List<Double> fetchVectorDoubleList(String strToSearch, String uri,int connectTimeout,int readTimeout) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(connectTimeout);//20 ms timeout
        con.setReadTimeout(readTimeout); //50ms timeout
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);
        byte[] json = Utils.toJSON(singletonMap("text", strToSearch));
        try (OutputStream os = con.getOutputStream()) {
            os.write(json, 0, json.length);
        }
        return (List<Double>) Utils.fromJSON(con.getInputStream());

    }

    public static List<Double> fetchVectorDoubleList(String strToSearch, String uri) throws IOException {
        return fetchVectorDoubleList(strToSearch,uri,CONNECT_TIMEOUT,READ_TIMEOUT);
    }

    public static List<String> fetchVectorStringList(String strToSearch, String uri) throws IOException {
        List<Double> list = fetchVectorDoubleList(strToSearch, uri);
        return list.stream().map(Object::toString).collect(Collectors.toList());
    }

    public static float[] fetchVectorFloatArray(String strToSearch, String uri,int connectTimeout,int readTimeout) throws IOException {
        List<Double> list = fetchVectorDoubleList(strToSearch, uri,connectTimeout,readTimeout);
        float[] res = new float[list.size()];
        int i = 0;
        for (Double d : list) res[i++] = d.floatValue();
        return res;
    }


    public static float[] fetchVectorFloatArray(String strToSearch, String uri) throws IOException {
        return fetchVectorFloatArray(strToSearch,uri,CONNECT_TIMEOUT,READ_TIMEOUT);
    }
}
