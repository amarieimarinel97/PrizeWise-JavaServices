package com.tuiasi.central_module.service;

import com.tuiasi.crawler_module.model.StockContext;
import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.crawler_module.model.Article;
import com.tuiasi.central_module.model.utils.SentimentAnalysisResult;
import com.tuiasi.central_module.model.StockEvolution;
import com.tuiasi.central_module.model.StockAnalysis;
import com.tuiasi.utils.AlgorithmServerAddress;
import com.tuiasi.utils.MathOperationsUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class AlgorithmService {

    @Autowired
    AlgorithmServerAddress algorithmServerAddress;

    public void handlePredictionBasedOnHistory(StockAnalysis stockInfo, int days) {
        String uri = algorithmServerAddress.pythonAlgorithmServerAddress + CONTEXT_ANALYSIS_PATH;

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("symbol", stockInfo.getStock().getSymbol());
        jsonObject.put("days", days);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<StockEvolution> result = restTemplate.postForEntity(uri, jsonObject, StockEvolution.class);
        if (result.hasBody()) {
            stockInfo.setStockEvolution(result.getBody());
        } else
            throw new ObjectNotFoundException("Stock evolution information not found.");
        stockInfo.getStock().setHistoryOptimismCoefficient(handleHOC(result.getBody().getPercentageChanges()));
    }

    public void handleStockContextPrediction(StockAnalysis stockInfo, int days) {
        String uri = algorithmServerAddress.pythonAlgorithmServerAddress + CONTEXT_ANALYSIS_PATH;

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("symbol", "DIA");
        jsonObject.put("days", days);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<StockEvolution> result = restTemplate.postForEntity(uri, jsonObject, StockEvolution.class);
        if (result.hasBody()) {
            stockInfo.setStockEvolution(result.getBody());
        } else
            throw new ObjectNotFoundException("Stock evolution information not found.");
        stockInfo.setStockContext(
                StockContext.builder()
                        .symbol(stockInfo.getStock().getSymbol())
                        .indicesPrediction(handleIndicesPrediction(result.getBody().getPercentageChanges()))
                        .sectorPrediction(handleIndicesPrediction(result.getBody().getPercentageChanges()))
                        .build()
        );
    }

    private Double handleIndicesPrediction(Double[] percentageChanges) {
        if (Objects.isNull(percentageChanges) || percentageChanges.length < 1)
            return DEFAULT_SCALE_NEUTRAL;
        return DEFAULT_SCALE_NEUTRAL + percentageChanges[1];
    }

    private Double handleHOC(Double[] changes) {
        Double result = MathOperationsUtils.generateHOCBasedOnNormalDistribution(changes);
        result = result / 2 + 5; //scale in [0,10]
        result = result > 10 ? 10 : result;
        result = result < 0 ? 0 : result;
        return result;
    }

    public double[] getSentimentAnalysis(String[] text) {
        String uri = algorithmServerAddress.pythonAlgorithmServerAddress + TEXTUAL_ANALYSIS_PATH;

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("text", text);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<SentimentAnalysisResult> result = restTemplate.postForEntity(uri, jsonObject, SentimentAnalysisResult.class);
        return result.getBody().getSentiment_analysis();
    }


    public static double getAverage(double[] input) {
        if (input.length == 0)
            return 0.5;

        double sum = 0.0;
        for (double number : input)
            sum = sum + number;
        return sum / input.length;
    }

    public void getArticlesSentimentAnalysis(StockAnalysis stockAnalysis) {
        List<Article> articleList = new ArrayList<>(stockAnalysis.getArticles());
        List<String> textToSendToAnalysis = new ArrayList<>();
        for (Article art : articleList) {
            textToSendToAnalysis.add(art.getTitle());
        }
        double[] sentimentAnalysisResult = this.getSentimentAnalysis(textToSendToAnalysis.toArray(new String[0]));
        for (int i = 0; i < sentimentAnalysisResult.length; ++i)
            articleList.get(i).setSentimentAnalysis(sentimentAnalysisResult[i]);

        stockAnalysis.getStock().setNewsOptimismCoefficient(getAverage(sentimentAnalysisResult) * 10);
    }

    private final String TEXTUAL_ANALYSIS_PATH = "/textual";
    private final String CONTEXT_ANALYSIS_PATH = "/regression";
    private final Double DEFAULT_SCALE_NEUTRAL = 5.0;
}


