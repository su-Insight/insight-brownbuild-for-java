package classification;

import base.XGBoostClassifier;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static base.TFIDFVectorizer.extractSparseMatrix;
import static classification.Metrics.computeMetrics;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/25 下午10:42
 * @Version: 1.0
 */
public class ClassificationXGBoost {
    public static XGBoostClassifier predictXGBoost(Map<String, Map<String, Object>> sets) throws Exception {
        XGBoostClassifier xgBoostClassifier = new XGBoostClassifier(sets);
        xgBoostClassifier.buildClassifier(null);
        return xgBoostClassifier;
    }

    public static Map<String, Map<String, Object>> classifyXGBoost(Map<String, Map<String, Object>> sets) throws Exception {
        // ### FIRST MODEL ###
        XGBoostClassifier model1 = predictXGBoost(sets);

        // ### SECOND MODEL ###
        List<String> addList = Arrays.asList("rerun", "commit_since_flaky");

        double[] standardDeviations = calculateColumnStandardDeviations(model1.shap.get("train"));
        List<Integer> selectColumn = selectNonZeroStandardDeviations(standardDeviations);

        Map<String, Map<String, Object>> secondSets = new HashMap<>();
        sets.forEach((key, value) -> {
            double[][] shap = model1.shap.get(key);
            List<Map<String, Integer>> infos = (List<Map<String, Integer>>) value.get("info");
            List<List<Double>> matrix = generateXMatrix(shap, addList, selectColumn, infos);
            secondSets.put(key, new HashMap<String, Object>(){{
                put("X", extractSparseMatrix(matrix));
                put("Y", value.get("Y"));
            }});
        });

        XGBoostClassifier model2 = predictXGBoost(secondSets);

        Map<String, Map<String, Object>> big = new HashMap<>();
        // 进行加权平均
        float[] prediction1 = model1.predictionProbability[0];
        float[] prediction2 = model2.predictionProbability[0];
        for (int alpha = 0; alpha < 110; alpha += 10) {
            for (int beta = 10; beta < 100; beta += 10) {
                List<Double> predictionProbabilityRange = new ArrayList<>();
                List<Integer> predictionRange = new ArrayList<>();
                for (int k = 0; k < prediction1.length; k++) {
                    predictionProbabilityRange.add((prediction1[k] * (100. - beta) + prediction2[k] * beta) / 100.);
                }
                for (double e : predictionProbabilityRange) {
                    predictionRange.add(e >= alpha / 100. ? 1 : 0);
                }

                String id = String.format("%.1fvar_%dtresh", (float)beta, alpha);

                Map<String, Double> resultRange = computeMetrics(sets, predictionRange);

                big.put(id, new HashMap<String, Object>(){{
                    put("prediction", predictionProbabilityRange);
                    put("result", resultRange);
                }});
            }
        }
        return big;
    }

    private static List<List<Double>> generateXMatrix(double[][] trainShap, List<String> addList, List<Integer> selectColumn, List<Map<String, Integer>> infos) {
        double[][] array1 = selectColumn.stream()
                .mapToInt(Integer::intValue)
                .mapToObj(columnIndex ->
                        Arrays.stream(trainShap).mapToDouble(doubles -> doubles[columnIndex])
                                .toArray())
                .toArray(double[][]::new);
        double[][] array2 = new double[infos.size()][addList.size()];
        AtomicInteger k = new AtomicInteger(0);
        infos.forEach(row -> {
            double[] line = array2[k.getAndIncrement()];
            AtomicInteger i = new AtomicInteger(0);
            row.forEach((key, value) -> {
                if (addList.contains(key)) {
                    line[i.getAndIncrement()] = value;
                }
            });
        });

        return arrayToList((double[][]) ArrayUtils.addAll(array1, array2));
    }

    private static double[] calculateColumnStandardDeviations(double[][] matrix) {
        int numCols = matrix[0].length;
        double[] stdDeviations = new double[numCols];

        for (int col = 0; col < numCols; col++) {
            double[] columnValues = new double[matrix.length];
            for (int row = 0; row < matrix.length; row++) {
                columnValues[row] = matrix[row][col];
            }

            StandardDeviation stdDeviationCalculator = new StandardDeviation();
            stdDeviations[col] = stdDeviationCalculator.evaluate(columnValues);
        }

        return stdDeviations;
    }

    private static List<Integer> selectNonZeroStandardDeviations(double[] stdDeviations) {
        List<Integer> indexes = IntStream.range(0, stdDeviations.length)
                .filter(i -> stdDeviations[i] != 0.0)
                .boxed()
                .collect(Collectors.toList());

        return indexes;
    }

    private static List<List<Double>> arrayToList(double[][] array) {
        List<List<Double>> resultList = new ArrayList<>();

        for (double[] row : array) {
            List<Double> rowList = new ArrayList<>();
            for (double value : row) {
                rowList.add(value);
            }
            resultList.add(rowList);
        }

        return resultList;
    }
}
