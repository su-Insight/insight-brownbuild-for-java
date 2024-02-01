package classification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import base.XGBoostClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.*;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/25 下午10:43
 * @Version: 1.0
 */
public class Metrics {
    /**
     * @Description Computes the performance metrics (precision/recallspecificity/f1) on the prediction regarding the actual label.
     * @Param [sets, predictionRange]
     * predictionRange: list of prediction value between 0.0 and 1.0.
     * @Return dictionnary containing all the performance metrics. Keys= name of the metrics and value= the value of the metric.
     * @Since version 1.0
     */
    public static Map<String, Double> computeMetrics(Map<String, Map<String, Object>> sets, List<Integer> predictionRange) throws Exception {
        // y : list of true labels (1 for flaky, 0 for safe).
        List<Integer> yValues = (List<Integer>) sets.get("test").get("Y");
        List<Integer> predictionRound = new ArrayList<>();

        for (Integer value : predictionRange) {
            predictionRound.add(Math.round(value));
        }

        Evaluation evaluation = getEvaluation(yValues, predictionRound, sets);
        double accuracy = evaluation.pctCorrect();
        double precision = evaluation.precision(0);
        double recall = evaluation.recall(0);
        double f1 = evaluation.fMeasure(0);
        List<Integer> newYValues = yValues.stream()
                                    .map(e -> 1 - e)
                                    .collect(Collectors.toList());
        List<Integer> newPredictionRange = predictionRange.stream()
                                                .map(e -> 1 - e)
                                                .collect(Collectors.toList());
        double specificity = getEvaluation(newYValues, newPredictionRange, sets).recall(0);

        return new HashMap<String, Double>(){{
            put("accuracy", accuracy);
            put("precision", precision);
            put("recall", recall);
            put("f1", f1);
            put("specificity", specificity);
        }};
    }

    private static Evaluation getEvaluation(List<Integer> y, List<Integer> predRound, Map<String, Map<String, Object>> sets) throws Exception {
        // 将标签列表转换为 Instances 对象
        Instances predictedInstances = convertListToInstance(predRound, y);
        // 创建 Evaluation 对象
        Evaluation evaluation = new Evaluation(predictedInstances);

        Classifier classifier = new XGBoostClassifier(sets);
        classifier.buildClassifier(predictedInstances);
        // 评估模型
        evaluation.evaluateModel(classifier, predictedInstances);
        return evaluation;
    }

    public static Instances convertListToInstance(List<Integer> pre, List<Integer> data) {
        // 创建多个整数属性的 Instances 对象
        FastVector attributes = new FastVector();
        for (int i = 0; i < pre.size(); i++) {
            attributes.addElement(new Attribute("IntegerAttribute_" + i));
        }

        // 创建 类别 的属性信息
        FastVector classValues = new FastVector();
        classValues.addElement("safe");
        classValues.addElement("flaky");
        Attribute classAttribute = new Attribute("Class", classValues);
        attributes.addElement(classAttribute);

        Instances instances = new Instances("Instance", attributes, 0);


        // 将整数值设置为 Instance 的特征值
        for (int i = 0; i < data.size(); i++) {
            Instance instance = new SparseInstance(data.size());
            instance.setValue((Attribute) attributes.elementAt(i), data.get(i));
            instance.setValue(classAttribute, pre.get(i) == 0 ? "safe" : "flaky");
            instances.add(instance);
        }

        instances.setClassIndex(data.size());

        return instances;
    }
}
