package com.tuiasi.service;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.*;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class AlgorithmService {
    public double getPredictionBasedOnHistory(StockInformation stockInfo, int days) {
        String uri = "http://127.0.0.1:8081/stock_regr";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("symbol", stockInfo.getStock().getSymbol());
        jsonObject.put("days", days);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<StockEvolution> result = restTemplate.postForEntity(uri, jsonObject, StockEvolution.class);
        if (result.hasBody())
            stockInfo.setStockEvolution(result.getBody());
        else
            throw new ObjectNotFoundException("Stock evolution information not found.");

        return result.getBody().getChanges()[1]*10+5; //TODO: HERE TO CHANGE HOC
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
        return 0;
    }

    public static double getAverage(double[] input) {
        if (input.length == 0)
            return 0.5;

        double sum = 0.0;
        for (double number : input)
            sum = sum + number;
        return sum / input.length;
    }

    public double getArticlesSentimentAnalysis(Set<Article> articleSet, boolean isSentAsString) {
        List<Article> articleList = new ArrayList<>(articleSet);
        if (isSentAsString) {
            StringBuilder sb = new StringBuilder();
            for (Article art : articleList)
                sb.append(art.getTitle()).append("|");
            return this.getSentimentAnalysis(sb.toString());
        } else {
            List<String> textToSendToAnalysis = new ArrayList<>();
            for (Article art : articleList) {
                textToSendToAnalysis.add(art.getTitle());
            }
            double[] sentimentAnalysisResult = this.getSentimentAnalysis(textToSendToAnalysis.toArray(new String[0]));
            for (int i = 0; i < sentimentAnalysisResult.length; ++i)
                articleList.get(i).setSentimentAnalysis(sentimentAnalysisResult[i]);
            return getAverage(sentimentAnalysisResult);
        }
    }


}
