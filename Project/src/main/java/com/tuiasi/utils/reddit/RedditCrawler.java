package com.tuiasi.utils.reddit;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.Article;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class RedditCrawler {

    private String lastCrawledArticleId;

    public Set<Article> crawlSubReddit(String subreddit, int noOfPages) {

        HashSet<Article> articles = new HashSet<>();
        Document doc = null;
        try {
            doc = Jsoup.connect("http://www.reddit.com/r/" + subreddit + "/top").get();
        } catch (IOException e) {
            log.error("Subreddit " + subreddit + " not found.");
            throw new ObjectNotFoundException();
        }
        Elements mainBody = doc.select(convertXPathToJsoupSyntax("/html/body/div[1]/div/div/div"));
        crawlSubRedditArticles(subreddit, mainBody, articles);

        if (noOfPages > 1)
            lastCrawledArticleId = getLastArticleId(doc);
        while (noOfPages-- > 1)
            articles.addAll(crawlNextSubRedditPage(subreddit));
        return articles;
    }

    public Set<Article> crawlNextSubRedditPage(String subreddit) {
        HashSet<Article> articles = new HashSet<>();
        Document doc = null;

        try {
            doc = Jsoup.connect("http://www.reddit.com/r/" + subreddit + "/top?after=" + lastCrawledArticleId).get();
            lastCrawledArticleId = getLastArticleId(doc);
        } catch (IOException e) {
            log.error("Subreddit " + subreddit + " not found.");
            throw new ObjectNotFoundException();
        }
        Elements mainBody = doc.select(convertXPathToJsoupSyntax("/html/body/div[1]/div/div/div"));
        crawlSubRedditArticles(subreddit, mainBody, articles);
        return articles;
    }

    private String getLastArticleId(Document doc) {
        Elements articleIds = doc.select("div[id^=t3_]:not(div[id*=-])");
        int count = articleIds.size();
        return articleIds.stream().skip(count - 1).findFirst().orElseThrow(ObjectNotFoundException::new).attr("id");
    }

    private void crawlSubRedditArticles(String subreddit, Elements mainBody, Set<Article> articles) {

        Elements links = mainBody.select("a[href^=/r/" + subreddit + "/comments/]");
        for (Element element : links) {
                  articles.add(Article.builder()
                            .title(element.select(convertXPathToJsoupSyntax("/a/div/h3")).html())
                            .build());
        }

        for (Article article : articles) {
            ArticleCrawlingWorker worker = ArticleCrawlingWorker.builder()
                    .article(article)
                    .build();
            worker.start();

            try {
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeToFile(String fileName, String str, boolean append) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, append));
        writer.write(str);
        writer.close();
    }

    private String convertXPathToJsoupSyntax(String xpath) {
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
