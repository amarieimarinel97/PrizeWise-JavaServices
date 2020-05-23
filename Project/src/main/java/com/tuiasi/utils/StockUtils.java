package com.tuiasi.utils;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.Article;
import com.tuiasi.model.StockSymbol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Component
@Slf4j
public class StockUtils {
    public String[] searchStockByCompany(String searchKey) {
        final String uri = "http://d.yimg.com/autoc.finance.yahoo.com/autoc?query=" + searchKey + "&region=1&lang=en&callback=YAHOO.Finance.SymbolSuggest.ssCallback";

        String[] symbols = new RestTemplate().getForObject(uri, String.class).split("\"");

        int i;
        String symbol = "";
        String companyName = "";
        for (i = 0; i < symbols.length; ++i) {
            if (symbols[i].equals("symbol")) {
                symbol = symbols[i + 2];
                companyName = symbols[i + 6];
                break;
            }
        }

        if (i == symbols.length)
            throw new ObjectNotFoundException("Stock symbol not found.");

        return new String[]{symbol, companyName};
    }

    public List<StockSymbol> randomlyFilterOutElements(List<StockSymbol> input, int noOfElements) {
        Random rand = new Random();
        List<StockSymbol> output = new ArrayList<>();
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


    public static void writeToFile(String fileName, String str, boolean append) {
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


    public static String convertXPathToJsoupSyntax(String xpath) {
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


}
