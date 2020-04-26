package com.tuiasi.threading.threads;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.Recommendation;
import com.tuiasi.model.Stock;
import com.tuiasi.threading.NotifyingThread;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        Iterator<Element> recommendationsIterator = document.select("div.box > div.hidden-xs tr > td.h5, div.box > div.hidden-xs tr > td:eq(4)").iterator();

        List<Recommendation> recommendations = new ArrayList<>();
        while (recommendationsIterator.hasNext()) {
            Recommendation recommendation = Recommendation.builder().build();
            String nextElement = recommendationsIterator.next().text();
            if (isDateFormat(nextElement)) {
                recommendation.setDate(getDate(nextElement));
                recommendation.setText(recommendationsIterator.next().text());
            } else {
                recommendation.setText(nextElement);
                recommendation.setDate(getDate(recommendationsIterator.next().text()));
            }
            recommendations.add(recommendation);
        }


        Double ERC1 = computeExpertsRecommendationCoefficient(recommendations);
        ERC1 = ERC1 > 5 ? 5 : ERC1;
        String theirRating = document.select("div.rating-label").text();
        Double ERC2 = theirRating.isEmpty() ? ERC1 : 5 - 2.5 * (Double.parseDouble(theirRating) - 1);
        stock.setExpertsRecommendationCoefficient(theirRating.isEmpty() ? ERC1 * 2 : (ERC1 + ERC2));
    }

    private Date getDate(String dateStr) {
        String[] mmddyy = dateStr.split("/");
        Date result = new Date();
        result.setMonth(Integer.parseInt(mmddyy[0].substring(Math.max(mmddyy[0].length() - 2, 0))) - 1);
        result.setDate(Integer.parseInt(mmddyy[1]));
        result.setYear(100 + Integer.parseInt(mmddyy[2].substring(0, Math.min(mmddyy[2].length(), 2))));
        return result;

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
                case "upgraded to market outperform":
                case "upgraded to buy":
                    currentCoefficient = 5.0;
                    break;
                case "upgraded to overweight":
                    currentCoefficient = 2.5;
                    break;
                case "hold":
                    currentCoefficient = 0.0;
                    break;
                case "downgraded to underweight":
                    currentCoefficient = -2.5;
                    break;
                case "downgraded to market underperform":
                case "downgraded to sell":
                    currentCoefficient = -5.0;
                    break;
                default:
                    continue;
            }
            currentRecommendation.setPoints(currentCoefficient * 1.5 * getDateDiff(maxDate, currentRecommendation.getDate(), TimeUnit.DAYS) / daysInterval);
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