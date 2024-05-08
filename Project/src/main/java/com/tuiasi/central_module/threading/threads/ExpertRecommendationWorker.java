package com.tuiasi.central_module.threading.threads;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.crawler_module.model.utils.Recommendation;
import com.tuiasi.crawler_module.model.Stock;
import com.tuiasi.central_module.threading.NotifyingThread;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpertRecommendationWorker extends NotifyingThread {
    private Stock stock;

    public ExpertRecommendationWorker(Stock stock) {
        this.stock = stock;
    }

    @Override
    public void doRun() {
        crawlExpertsRecommendationCoefficient(this.stock);
    }

    private void crawlExpertsRecommendationCoefficient(Stock stock) {
        Document document = null;
        try {
            document = Jsoup.connect("http://markets.businessinsider.com/analyst/" + stock.getSymbol()).get();
        } catch (IOException e) {
            throw new ObjectNotFoundException("Symbol " + stock.getSymbol() + " not found.");
        }
        Iterator<Element> recommendationsIterator = document.select("div#analyst_opinions tbody.table__tbody > tr").iterator();

        List<Recommendation> recommendations = new ArrayList<>();
        while (recommendationsIterator.hasNext()) {
            String nextElement = recommendationsIterator.next().text();
            Recommendation recommendation = computeRecommendation(nextElement);
            recommendations.add(recommendation);
        }

        Double ERC1 = computeExpertsRecommendationCoefficient(recommendations);
        String theirRating = document.select("div.moodys-rating__container > span.moodys-rating__rating").text();
        Double[] oldInterval = new Double[]{5.0, 1.0};
        Double[] newInterval = new Double[]{0.0, 10.0};

        Double ERC2 = theirRating.isEmpty() ? ERC1 : computeERC2(Double.parseDouble(theirRating), oldInterval, newInterval);
        Double result = theirRating.isEmpty() ? ERC1 : (ERC1 + ERC2) / 2.0;
        result = result < 0 ? 0 : result > 10 ? 10 : result;
        stock.setExpertsRecommendationCoefficient(result);
    }

    private Recommendation computeRecommendation(String recommendationText) {
        Recommendation recommendation = Recommendation.builder().build();
        String[] tokens = recommendationText.split(" ");
        recommendation.setDate(getDate(recommendationText));
        recommendation.setText(tokens[tokens.length - 3]);
        return recommendation;
    }

    private Double computeERC2(Double x, Double[] oldInterval, Double[] newInterval) {
        Double minOld = oldInterval[0], maxOld = oldInterval[1];
        Double minNew = newInterval[0], maxNew = newInterval[1];
        return (maxNew - minNew) / (maxOld - minOld) * (x - maxOld) + maxNew;
    }

    private static final String DATE_REGEX = "\\b([0-9]{1,2})\\/([0-9]{1,2})\\/([0-9]{2,4})\\b";

    private Date getDate(String input) {

        Matcher matcher = Pattern.compile(DATE_REGEX).matcher(input);
        if (matcher.find()) {
            String match = matcher.group();
            String[] mmddyy = match.split("/");
            Date result = new Date();
            result.setMonth(Integer.parseInt(mmddyy[0].substring(Math.max(mmddyy[0].length() - 2, 0))) - 1);
            result.setDate(Integer.parseInt(mmddyy[1]));
            result.setYear(100 + Integer.parseInt(mmddyy[2].substring(0, Math.min(mmddyy[2].length(), 2))));
            return result;
        }

        throw new ObjectNotFoundException("Could not retrieve date from input");
    }

    private boolean isDateFormat(String input) {
        return input.contains("/");
    }

    private Double computeExpertsRecommendationCoefficient(List<Recommendation> recommendations) {
        Double totalRecommendationPoints = 0.0;
        Double totalRecommendations = 0.0;

        recommendations.sort(Recommendation.comparatorByDate);
        Date minDate = recommendations.get(0).getDate();

        Date maxDate = new Date();
        long daysInterval = getDateDiff(maxDate, minDate, TimeUnit.DAYS);
        daysInterval = daysInterval == 0 ? 1 : daysInterval;

        for (Recommendation currentRecommendation : recommendations) {
            double currentCoefficient = 0.0;
            switch (currentRecommendation.getText().toLowerCase()) {
                case "buy":
                    currentCoefficient = 9.0;
                    break;
                case "hold":
                    currentCoefficient = 5.0;
                    break;
                case "sell":
                    currentCoefficient = 1.0;
                    break;
                default:
                    continue;
            }
            Double dateWeightedCoefficient = currentCoefficient - (currentCoefficient - 5.0) * (getDateDiff(maxDate, currentRecommendation.getDate(), TimeUnit.DAYS) / daysInterval);
            currentRecommendation.setPoints(0.7 * currentCoefficient + 0.3 * dateWeightedCoefficient);
            totalRecommendationPoints += currentRecommendation.getPoints();
            ++totalRecommendations;
        }
        return totalRecommendationPoints / totalRecommendations;
    }

    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillis = date1.getTime() - date2.getTime();
        return timeUnit.convert(diffInMillis, TimeUnit.MILLISECONDS);
    }
}