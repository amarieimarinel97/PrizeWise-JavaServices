package com.tuiasi.central_module.threading.threads;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuiasi.central_module.model.StockAnalysis;
import com.tuiasi.central_module.service.AlgorithmService;
import com.tuiasi.central_module.threading.NotifyingThread;
import com.tuiasi.crawler_module.model.Post;
import com.tuiasi.crawler_module.model.SocialNetwork;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.toList;

@Slf4j
public class SocialScraperWorker extends NotifyingThread {


    private StockAnalysis stockAnalysis;
    private AlgorithmService algorithmService;

    public SocialScraperWorker(AlgorithmService algorithmService, StockAnalysis stockAnalysis) {
        this.stockAnalysis = stockAnalysis;
        this.algorithmService = algorithmService;
    }

    @Override
    public void doRun() {
        List<Post> posts = new ArrayList<>();
        posts.addAll(scrapeTwitterPosts());
        posts.addAll(scrapeRedditPosts());
        postsSentimentAnalysis(posts);

        stockAnalysis.setPosts(posts);
        stockAnalysis.getStock().setPosts(posts);
        stockAnalysis.getStock().setLastUpdated(new Date());
    }


    private void postsSentimentAnalysis(List<Post> posts) {

        List<String> textToAnalyse = posts.stream().map(post -> post.getTitle() + " " + post.getBody()).collect(toList());
        double[] sentimentAnalysis = algorithmService.getSentimentAnalysis(textToAnalyse);

        for (int i = 0; i < textToAnalyse.size(); i++)
            posts.get(i).setSentimentAnalysis(sentimentAnalysis[i]);
    }

    private List<Post> scrapeTwitterPosts() {

        Map<String, String> twitterParams = getTwitterParams();
        String params = getParamsString(twitterParams);

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(API_URL + params, String.class);

        return getPosts(response, SocialNetwork.TWITTER);
    }

    private List<Post> scrapeRedditPosts() {

        Map<String, String> twitterParams = getRedditParams();
        String params = getParamsString(twitterParams);

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(API_URL + params, String.class);

        return getPosts(response, SocialNetwork.REDDIT);
    }

    private List<Post> getPosts(String response, SocialNetwork socialNetwork) {
        List<Post> posts = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(response);
            JsonNode items = root.path("items");

            for (JsonNode item : items) {
                String title = item.path("title").asText();
                String snippet = item.path("snippet").asText();
                posts.add(Post.builder().title(title).body(snippet).stock(stockAnalysis.getStock()).socialNetwork(socialNetwork).build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return posts;
    }

    private Map<String, String> getTwitterParams() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key", "AIzaSyAWTiLtKxQSZ4OtT1SDIZF37cJjkbv2jok");
        parameters.put("cx", "616cf1495cbaa42e2");
        parameters.put("q", "inurl:status");
        parameters.put("dateRestrict", "m1");
        parameters.put("exactTerms", stockAnalysis.getStock().getSymbol() + " stock");
        parameters.put("siteSearch", "twitter.com");
        return parameters;
    }

    private Map<String, String> getRedditParams() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key", "AIzaSyAWTiLtKxQSZ4OtT1SDIZF37cJjkbv2jok");
        parameters.put("cx", "616cf1495cbaa42e2");
        parameters.put("q", "inurl:comments");
        parameters.put("dateRestrict", "m1");
        parameters.put("exactTerms", stockAnalysis.getStock().getSymbol() + " stock");
        parameters.put("siteSearch", "reddit.com");
        return parameters;
    }

    private String getParamsString(Map<String, String> params) {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(entry.getKey());
            result.append("=");
            result.append(entry.getValue());
            result.append("&");
        }

        String resultString = result.toString();
        return !resultString.isEmpty()
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }


    private static final DateTimeFormatter formatter = ofPattern("yyyy-MM-dd", ENGLISH);
    private static final String BASE_URL = "http://google.com/search?q=";
    private static final String twitterPath = "+stock\"+site%3Atwitter.com+inurl%3Astatus";
    private static final String redditPath = "\"+site%3Areddit.com+inurl%3Acomments";
    private static final String API_URL = "https://www.googleapis.com/customsearch/v1?";
}
