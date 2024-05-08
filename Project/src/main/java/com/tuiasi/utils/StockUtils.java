package com.tuiasi.utils;

import com.tuiasi.central_module.model.StockAnalysis;
import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.crawler_module.model.Article;
import com.tuiasi.crawler_module.model.StockContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Slf4j
public class StockUtils {
    public String[] searchStockByCompany(String searchKey) {
        final String uri = SYMBOL_URL_BEGINNING + encodeInput(searchKey) + SYMBOL_URL_ENDING;
//        String[] symbols = new RestTemplate().getForObject(uri, String.class).split("\"");
//
//        int i;
//        String symbol = "";
//        String companyName = "";
//        boolean symbolFound = false;
//        for (i = 0; i < symbols.length; ++i) {
//            if (symbols[i].equals("symbol")) {
//                symbol = symbols[i + 2];
//                companyName = symbols[i + 6];
//                symbolFound = true;
//                break;
//            }
//        }
//
//        if (!symbolFound || !symbol.matches("[a-zA-Z0-9]+"))
//            throw new ObjectNotFoundException("Stock symbol not found: " + searchKey);

        return new String[]{searchKey, searchKey};
    }

    private String encodeInput(String input) {
        String value = (input + " ").split(" ")[0];
        value = (value + ".").split("\\.")[0];
        return value;
    }

    public Double computeERCFromStockAnalysis(StockAnalysis stockAnalysis) {
        return 0.5 * stockAnalysis.getStock().getExpertsRecommendationCoefficient() + 0.5 * (stockAnalysis.getStock().getExpertsRecommendationCoefficient() + stockAnalysis.getStock().getHistoryOptimismCoefficient() + stockAnalysis.getStock().getNewsOptimismCoefficient()) / 3.0;
    }

    public List<StockContext> randomlyFilterOutElements(List<StockContext> input, int noOfElements) {
        Random rand = new Random();
        List<StockContext> output = new ArrayList<>();
        for (int i = 0; i < noOfElements; ++i) {
            int randomIndex = rand.nextInt(input.size());
            output.add(input.get(randomIndex));
            input.remove(randomIndex);
        }
        return output;
    }

    public Set<Article> sortBySentimentAnalysis(Set<Article> articles) {
        Set<Article> sortedArticles = new TreeSet<>((art1, art2) -> {
            if (art1 == null || art2 == null || art1.getSentimentAnalysis() == null || art2.getSentimentAnalysis() == null)
                return 0;
            return art1.getSentimentAnalysis() < art2.getSentimentAnalysis() ? 1 : -1;
        });
        sortedArticles.addAll(articles);
        return sortedArticles;
    }


    public void writeToFile(String fileName, String str, boolean append) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(fileName, append));
            writer.write(str);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public String convertXPathToJsoupSyntax(String xpath) {
        if (xpath.charAt(0) == '/')
            xpath = xpath.substring(1);

        StringBuilder result = new StringBuilder();
        for (char c : xpath.toCharArray()) {
            switch (c) {
                case '[':
                    result.append(":eq(");
                    break;
                case ']':
                    result.append(")");
                    break;
                case '/':
                    result.append(" > ");
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }
    private final String SYMBOL_URL_BEGINNING = "http://d.yimg.com/autoc.finance.yahoo.com/autoc?query=";
    private final String SYMBOL_URL_ENDING = "&region=1&lang=en&callback=YAHOO.Finance.SymbolSuggest.ssCallback";

}
