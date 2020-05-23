package com.tuiasi.model.utils;

import lombok.Builder;
import lombok.Data;

import java.util.Comparator;
import java.util.Date;

@Data
@Builder
public class Recommendation {
    Double points;
    Date date;
    String text;

    public static final Comparator comparatorByDate =
            new Comparator() {
                @Override
                public int compare(Object rec1, Object rec2) {
                    if (!(rec1 instanceof Recommendation))
                        return -1;
                    if (!(rec2 instanceof Recommendation))
                        return 1;
                    if (((Recommendation) rec1).date.compareTo(((Recommendation) rec2).date) > 0)
                        return 1;
                    return -1;
                }
            };

    public static final String BUY = "BUY";
    public static final String OVERWEIGHT = "OVERWEIGHT";
    public static final String HOLD = "HOLD";
    public static final String UNDERWEIGHT = "UNDERWEIGHT";
    public static final String SELL = "SELL";
}
