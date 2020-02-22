package com.tuiasi.utils;

import com.tuiasi.model.Article;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.IOException;

@Builder
@Slf4j
@Data
public class ArticleCrawlingWorker extends Thread {

    private Article article;

    @Override
    public void run() {
        Elements articleBody = null;
        try {
            articleBody = Jsoup.connect("http://www.reddit.com" + article.getLink()).get().select("div[data-test-id=post-content] > div:eq(4)");
        } catch (IOException e) {
            log.info("Could not access article " + article.getLink() + ".");
            return;
        }
        article.setBody(articleBody.text());
        log.info("Article body: "+articleBody.text());
    }


}
