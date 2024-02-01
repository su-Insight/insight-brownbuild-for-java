package classification;

import base.DataFrame;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/25 下午10:42
 * @Version: 1.0
 */
public class Baseline {
    /**
     * @return
     * @Description Computes all baselines performance metrics (precision/recalléspecificity/f1).
     * See paper for each baseline's description.
     * @Param full dataset in a pandas dataframe format.
     * @Return dictionnary with keys=name of baseline and value=metrics of baseline. The metrics of a baseline is a
     * dictionary with keys= name of the metric and value= the value of the metric.
     * @Since version 1.0
     */
    public static Map<String, Map<String, Double>> baseline(DataFrame data){
        List<Object> status = data.getColumn("status");
        List<Integer> indexes = IntStream.range(0, status.size())
                        .filter(i -> status.get(i).equals(0))
                        .boxed()
                        .collect(Collectors.toList());
        List<Object> fails = data.getLocationData(indexes).getColumn("flaky");
        int brown = fails.stream()
                .filter(element -> element.equals("flaky")).mapToInt(e -> 1).sum();
        int trueFail = fails.stream()
                .filter(element -> !element.equals("flaky")).mapToInt(e -> 1).sum();

        double flakyRate = 1.0 * brown / (brown + trueFail);

        Map<String, Map<String, Double>> baseMetrics  = new HashMap<>();
        for (String key : Arrays.asList("random50", "randomB", "alwaysBrown")) {
            baseMetrics.put(key, baselineMetrics(flakyRate, key));
        }
        return baseMetrics;
    }

    /**
     * @Description Computes a baseline 'which' performance metrics (precision/recallspecificity/f1).
     * @Param [flakyRate, key]
     * flakyRate: flaky rate of failing jobs in the project's dataset.
     * key: name of the baseline (default=alwaysBrown).
     * @Return dictionnary containing all the performance metrics. Keys= name of the metrics and value= the value of the metric.
     * @Since version 1.0
     */
    private static Map<String, Double> baselineMetrics(double flakyRate, String key) {
        Map<String, Double> matrics;
        switch (key) {
            case "random50":
                matrics = new HashMap<String, Double>(){{
                    put("precision", flakyRate);
                    put("recall", .5);
                    put("specificity", .5);
                    put("f1", flakyRate / (1 + 2 * flakyRate));
                }};
            case "randomB":
                matrics = new HashMap<String, Double>(){{
                    put("precision", flakyRate);
                    put("recall", flakyRate);
                    put("specificity", 1 - flakyRate);
                    put("f1", flakyRate / 2);
                }};
            default:    // which == 'alwaysBrown'
                matrics = new HashMap<String, Double>(){{
                    put("precision", flakyRate);
                    put("recall", 1.0);
                    put("specificity", 0.0);
                    put("f1", flakyRate / (1 + flakyRate));
                }};
        }
        return matrics;
    }
}
