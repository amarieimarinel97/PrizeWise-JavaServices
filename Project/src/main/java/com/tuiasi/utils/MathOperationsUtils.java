package com.tuiasi.utils;

import java.util.ArrayList;
import java.util.List;

public class MathOperationsUtils {

    private static double normalDistributionFunction(double x) {
        return Math.exp(-((x - NORMAL_MEAN) * (x - NORMAL_MEAN)) / (2 * NORMAL_DEVIATION * NORMAL_DEVIATION)) / (Math.sqrt(2 * Math.PI) * NORMAL_DEVIATION);
    }

    private static List<Double> generateSubunitValuesAtFixedInterval(int noOfValues) {
        double interval = 1.0 / (noOfValues - 1);
        List<Double> result = new ArrayList<>();
        for (double i = 0; i <= 1; i += interval)
            result.add(i);
        return result;
    }

    private static List<Double> normalizeNumberList(List<Double> input) {
        double sum = input.stream().reduce(0.0, Double::sum);
        List<Double> output = new ArrayList<>();
        input.forEach(num -> output.add(num / sum));
        return output;
    }

    public static double generateHOCBasedOnNormalDistribution(Double[] input) {
        List<Double> coefficients = generateSubunitValuesAtFixedInterval(input.length-1);

        for (int i = 0; i < coefficients.size(); ++i)
            coefficients.set(i, normalDistributionFunction(coefficients.get(i)));
        coefficients = normalizeNumberList(coefficients);
        double result = 0;
        for (int i = 0; i < coefficients.size(); ++i)
            result += coefficients.get(i) * input[i+1];
        return result;
    }

    private static double NORMAL_DEVIATION = 0.4;
    private static double NORMAL_MEAN = 0;
}
