package com.tuiasi.service;

import com.tuiasi.configuration.ApplicationConfiguration;
import com.tuiasi.model.StockRegressionPrediction;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

@Service
@Slf4j
public class AlgorithmService {
    @Autowired
    private ApplicationConfiguration applicationConfiguration;

    public double getPredictionBasedOnHistory(String stock, int days) {
        String uri = "http://127.0.0.1:8081/stock_regr";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("symbol", stock);
        jsonObject.put("days", days);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<StockRegressionPrediction> result = restTemplate.postForEntity(uri, jsonObject, StockRegressionPrediction.class);
        return result.getBody().getChanges()[1];
    }
}
