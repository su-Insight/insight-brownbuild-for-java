package base;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/30 下午3:20
 * @Version: 1.0
 */
public class XGBoostClassifier extends Classifier {
    private Booster booster;
    public Map<String, double[][]> shap;
    public float[][] prediction;
    public float[][] predictionProbability;

    private Map<String, OpenMapRealMatrix> matrices;
    private Map<String, List<Integer>> labels;

    public XGBoostClassifier(Map<String, Map<String, Object>> sets) {
        matrices = new HashMap<String, OpenMapRealMatrix>(){{
            put("train", (OpenMapRealMatrix) sets.get("train").get("X"));
            put("valid", (OpenMapRealMatrix) sets.get("valid").get("X"));
            put("test", (OpenMapRealMatrix) sets.get("test").get("X"));
        }};

        labels = new HashMap<String, List<Integer>>(){{
            put("train", (List<Integer>) sets.get("train").get("Y"));
            put("valid", (List<Integer>) sets.get("valid").get("Y"));
        }};
    }

    @Override
    public void buildClassifier(Instances instances) throws Exception {
        OpenMapRealMatrix trainMatrix = matrices.get("train");
        CSR trainCSR = new CSR(trainMatrix.getData(), trainMatrix.getColumnDimension(), trainMatrix.getRowDimension());
        List<Integer> trainLabelsList = labels.get("train");
        assert trainLabelsList.size() == trainMatrix.getRowDimension();
        float[] trainLabels = new float[trainLabelsList.size()];
        for (int i = 0; i < trainLabelsList.size(); i++) {
            trainLabels[i] = trainLabelsList.get(i);
        }
        DMatrix dtrain = new DMatrix(trainCSR.rowHeaders, trainCSR.colIndex, trainCSR.data, DMatrix.SparseType.CSR, trainCSR.numColumn);
        dtrain.setLabel(trainLabels);

        OpenMapRealMatrix validMatrix = matrices.get("valid");
        CSR validCSR = new CSR(validMatrix.getData(), validMatrix.getColumnDimension(), validMatrix.getRowDimension());
        List<Integer> validLabelsList = labels.get("valid");
        assert validLabelsList.size() == validMatrix.getRowDimension();
        float[] validLabels = new float[validLabelsList.size()];
        for (int i = 0; i < validLabelsList.size(); i++) {
            validLabels[i] = validLabelsList.get(i);
        }
        DMatrix dvalid = new DMatrix(validCSR.rowHeaders, validCSR.colIndex, validCSR.data, DMatrix.SparseType.CSR, validCSR.numColumn);
        dvalid.setLabel(validLabels);

        OpenMapRealMatrix testMatrix = matrices.get("test");
        CSR testCsr = new CSR(testMatrix.getData(), testMatrix.getColumnDimension(), testMatrix.getRowDimension());
        DMatrix dtest = new DMatrix(testCsr.rowHeaders, testCsr.colIndex, testCsr.data, DMatrix.SparseType.CSR, testCsr.numColumn);

        Map<String, Object> params = new HashMap<>();
        params.put("objective", "binary:logistic"); // 二分类的逻辑回归
        params.put("eta", 1); // 学习率
        params.put("max_depth", 100);   // 树的最大深度
        params.put("silent", 1);    // 禁止输出训练信息
        params.put("maximize", true);   // 早停时最大化评估指标
        params.put("verbose_eval", 0);   // 关闭评估信息输出
        // params.put("num_class", 2);


        Map<String, DMatrix> watches = new HashMap<String, DMatrix>() {
            {
                put("train", dtrain);
                put("valid", dvalid);
            }
        };


        int earlyStoppingRounds = 3;
        int numBoostRound = 50;
        Booster booster = XGBoost.train(dtrain, params, numBoostRound, watches, null, null, null, earlyStoppingRounds,null);

        float[][] predictionProbability = booster.predict(dtest);

        float[][] prediction = Arrays.copyOf(predictionProbability, predictionProbability.length);
        for (int i = 0; i < prediction.length; i++) {
            float[] row = prediction[i];
            for (int j = 0; j < row.length; j++) {
                row[j] = Math.round(row[j]);
            }
        }

        Map<String, double[][]> shap = new HashMap<>();
        shap.put("train", new double[trainMatrix.getRowDimension()][300]);
        shap.put("valid", new double[validMatrix.getRowDimension()][300]);
        shap.put("test", new double[testMatrix.getRowDimension()][300]);

        this.shap = shap;
        this.booster = booster;
        this.prediction = prediction;
        this.predictionProbability = predictionProbability;
    }

    public Booster getBooster() {
        return booster;
    }

    @Override
    public double classifyInstance(Instance instance) throws Exception {
        return Math.round(booster.predict(instanceToDMatrix(instance))[0][0]);
    }

    @Override
    public double[] distributionForInstance(Instance instance) throws Exception {
        return super.distributionForInstance(instance);
    }

    public static DMatrix instanceToDMatrix(Instance wekaInstance) throws XGBoostError {
        // 提取实例的特征值
        float[] features = new float[wekaInstance.numAttributes()];
        for (int i = 0; i < wekaInstance.numAttributes(); i++) {
            features[i] = (float) wekaInstance.value(i);
        }

        // 创建 DMatrix 对象
        DMatrix dMatrix = new DMatrix(features, 1, features.length, Float.NaN);
        return dMatrix;
    }
}
