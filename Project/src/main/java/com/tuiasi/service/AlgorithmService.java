package com.tuiasi.service;

import com.tuiasi.configuration.ApplicationConfiguration;
import com.tuiasi.model.Article;
import com.tuiasi.model.SentimentAnalysisResult;
import com.tuiasi.model.StockPredictionResult;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
        ResponseEntity<StockPredictionResult> result = restTemplate.postForEntity(uri, jsonObject, StockPredictionResult.class);
        return result.getBody().getChanges()[1];
    }

    public double[] getSentimentAnalysis(String[] text) {
        String uri = "http://127.0.0.1:8081/sent_analysis";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("text", text);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<SentimentAnalysisResult> result = restTemplate.postForEntity(uri, jsonObject, SentimentAnalysisResult.class);
        return result.getBody().getSentiment_analysis();
    }

    public double getSentimentAnalysis(String text) {
        String uri = "http://127.0.0.1:8081/sent_analysis";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("text", text);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> result = restTemplate.postForEntity(uri, jsonObject, String.class);
        System.out.println("Received: " + result.getBody());
        return 0;
    }

    public double getArticlesSentimentAnalysis(Set<Article> articles, boolean isSentAsString) {
        if (isSentAsString) {
            StringBuilder sb = new StringBuilder();
            for (Article art : articles)
                sb.append(art.getTitle()).append("|");
            return this.getSentimentAnalysis(sb.toString());
        } else {
            List<String> textToSendToAnalysis = new ArrayList<>();
            for (Article art : articles) {
                textToSendToAnalysis.add(art.getTitle());
            }
            return Arrays.stream(this.getSentimentAnalysis(textToSendToAnalysis.toArray(new String[0])))
                    .average().orElse(0);
        }
    }


}
