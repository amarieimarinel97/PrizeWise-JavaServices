package com.tuiasi.service;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.Article;
import com.tuiasi.model.SentimentAnalysisResult;
import com.tuiasi.model.StockEvolution;
import com.tuiasi.model.StockInformation;
import com.tuiasi.utils.AlgorithmServerAddress;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AlgorithmService {

    @Autowired
    AlgorithmServerAddress algorithmServerAddress;

    public void handlePredictionBasedOnHistory(StockInformation stockInfo, int days) {
        String uri = algorithmServerAddress.pythonAlgorithmServerAddress + "/stock_regr";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("symbol", stockInfo.getStock().getSymbol());
        jsonObject.put("days", days);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<StockEvolution> result = restTemplate.postForEntity(uri, jsonObject, StockEvolution.class);
        if (result.hasBody())
            stockInfo.setStockEvolution(result.getBody());
        else
            throw new ObjectNotFoundException("Stock evolution information not found.");
        stockInfo.getStock().setHistoryOptimismCoefficient(handleHOC(result.getBody().getChanges()));
    }

    private Double handleHOC(Double[] changes) {
        Double sum = 0.0;
        for (Double d : changes) sum += d;
        Double result = sum / changes.length - 1;
        result = result / 2 + 5; //scale in [0,10]
        result = result > 10 ? 10 : result;
        result = result < 0 ? 0 : result;
        return result;
    }

    public double[] getSentimentAnalysis(String[] text) {
        String uri = algorithmServerAddress.pythonAlgorithmServerAddress + "/sent_analysis";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("text", text);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<SentimentAnalysisResult> result = restTemplate.postForEntity(uri, jsonObject, SentimentAnalysisResult.class);
        return result.getBody().getSentiment_analysis();
    }

    public double getSentimentAnalysis(String text) {
        String uri = algorithmServerAddress.pythonAlgorithmServerAddress + "/sent_analysis";

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

    public void getArticlesSentimentAnalysis(StockInformation stockInformation) {
        List<Article> articleList = new ArrayList<>(stockInformation.getArticles());
        List<String> textToSendToAnalysis = new ArrayList<>();
        for (Article art : articleList) {
            textToSendToAnalysis.add(art.getTitle());
        }
        double[] sentimentAnalysisResult = this.getSentimentAnalysis(textToSendToAnalysis.toArray(new String[0]));
        for (int i = 0; i < sentimentAnalysisResult.length; ++i)
            articleList.get(i).setSentimentAnalysis(sentimentAnalysisResult[i]);

        stockInformation.getStock().setNewsOptimismCoefficient(getAverage(sentimentAnalysisResult) * 10);
    }
}


