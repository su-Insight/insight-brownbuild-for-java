package preprocessing;

import base.*;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.ChiSquaredAttributeEval;
import weka.attributeSelection.Ranker;
import weka.core.*;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;


import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.*;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/6 下午5:35
 * @Version: 1.0
 */
public class Vectorization {
    private static final String ARFF_FILE_NAME = "dataset.arff";

    /**
     * @Description From the wordcount sets 'sets', generates a corpus of phrases only containing words from 'target'.
     *     If target==None, then creates a corpus with all the words in the sets.
     * @Param [data, target]
     * data: dictionary with keys=word and values=subsets.
     * target: list of words or None (default=None).
     * @Return string of words, respecting the word count in sets.
     * @Since version 1.0
     * The corpus is created by concatenating all the words in the wordcount set the number of times given in wordcount.
     *     Ex: sets = [{'a':3, 'b':1}, {'a':2, 'c':2}]
     *         target = ['a', 'b']
     *
     *         out = ['a a a b', 'a a']
     */
    public static List<String> setToCorpus(DataFrame data, List<String> target){
        List<Object> hereSets = data.getColumn("wordCount");

        List<String> corpus = new ArrayList<>();
        for (Object wordCount : hereSets) {
            Map<String, Integer> dic = (Map<String, Integer>) wordCount;
            List<String> _corpus = new ArrayList<>();
            if (target != null){
                for (String t : target) {
                    if (dic.containsKey(t)) {
                        _corpus.addAll(Collections.nCopies(dic.get(t), t));
                    }
                }
            }else {
                for (String key : dic.keySet()) {
                    _corpus.addAll(Collections.nCopies(dic.get(key), key));
                }
            }
            corpus.add(String.join(" ", _corpus));
        }
        return corpus;
    }
    /**
     * @Description Computes the tfidf metric for a dictionary of sets 'sets' where each value is a
     *     wordcount set, and the keys are the subsets names ('train', 'valid', 'test').
     *     'target' is a list of words that define the vocabulary to use for
     *     the computation (mask the other words).
     *     If 'only_train' is set to True, only consider the key='train' in the set. If
     *     not, consider all keys.
     * @Param [sets, target, onlyTrain]
     * sets: list of dictionaries with keys=train/valid/test and values=subsets.
     * target: list of words or None (default=None).
     * @Return java.util.List<java.util.Objects>
     * M: dictionary of keys=train/valid/test (or just train) and values=tfidf matrix
     * feature: list of words/features of the tfidf matrices (names of the columns)
     * Returns the tfidf matrices for all the considered keys in the 'sets' as a dictionary 'M' and a list of the feature
     * names of the tfidf matrices.
     * @Since version 1.0
     */
    public static TFIDFMatrix tf_idf(Map<String, DataFrame> sets, List<String> target, boolean onlyTrain){
        Map<String, List<String>> corpus = new HashMap<>();
        for (String key : sets.keySet()) {
            corpus.put(key, setToCorpus(sets.get(key), target));
        }

        TFIDFVectorizer vectorizer = new TFIDFVectorizer();
        vectorizer.fit(corpus.get("train"));
        List<String> featureNames = vectorizer.getFeatureNames();

        Map<String, OpenMapRealMatrix> tfidfMatrixes = new HashMap<>();
        tfidfMatrixes.put("train", vectorizer.transform(corpus.get("train")));

        if (!onlyTrain) {
            tfidfMatrixes.put("valid", vectorizer.transform(corpus.get("valid")));
            tfidfMatrixes.put("test", vectorizer.transform(corpus.get("test")));
        }

        return new TFIDFMatrix(featureNames, tfidfMatrixes);
    }
    /**
     * @Description Computes the TF-IDF matrices, following the paper's iterative vectorization approach.
     * @Param [experiment, sets]
     * experiment: Experiment object representing the current experiment set-up
     * sets: list of dictionaries with keys=train/valid/test and values=subsets.
     * @Return
     * M_tfidf: dictionary with keys=train/valid/test and values=tfidf matrix
     * features: list of words/features of the tfidf matrices (names of the columns)
     * @Since version 1.0
     */
    public static TFIDFMatrix xValues(Experiment experiment, Map<String, DataFrame> sets) throws Exception {
        // int iterSize = 1000; // size of the sub training sets
        int iterSize = sets.get("train").getColumnData().size();
        int n = (int) Math.ceil((double) sets.get("train").getColumnData().size() / iterSize);
        List<String> kSelected = new ArrayList<>();
        Map<String, DataFrame> subSet = new HashMap<>();
        TFIDFMatrix matrix;
        Map<String, List<Integer>> yTfidf;
        for (int i = 0; i < n; i++) {
            // generate tfidf matrices + kbest selecting for each sub training set
            List<Integer> indexes = IntStream.rangeClosed(i * iterSize, (i + 1) * iterSize - 1)
                    .boxed()
                    .collect(Collectors.toList());
            subSet.put("train", sets.get("train").getLocationData(indexes));
            matrix = tf_idf(subSet, null, true);
            // System.out.println(matrix);
            yTfidf = yValues(subSet);
            kSelected.addAll(Objects.requireNonNull(kBest(experiment, matrix.get("train"), matrix.getFeatureNames(), yTfidf.get("train"))));
        }

        // final kbest selection on the union of the preselected features
        kSelected = new ArrayList<>(new HashSet<>(kSelected));
        subSet.put("train", sets.get("train"));
        matrix = tf_idf(subSet, kSelected, true);
        yTfidf = yValues(subSet);
        List<String> finalKSelected = kBest(experiment, matrix.get("train"), matrix.getFeatureNames(), yTfidf.get("train"));

        // final tfidf matrices
        matrix = tf_idf(sets, finalKSelected, false);

        return matrix;
    }

    private static List<String> kBest(Experiment experiment, OpenMapRealMatrix matrix, List<String> target, List<Integer> classLabels) throws Exception {
        // 将 OpenMapRealMatrix 转换为 Weka 的 Instances 对象
        Instances wekaInstances = convertToWekaInstances(matrix, classLabels, target);
        ConverterUtils.DataSource source = new ConverterUtils.DataSource("dataset.arff");
        Instances dataset = source.getDataSet();
        // 创建 Ranker 对象
        Ranker ranker = new Ranker();
        ranker.setNumToSelect(experiment.kbestThresh); // 选择前k个特征

        ChiSquaredAttributeEval chi2 = new ChiSquaredAttributeEval();
        // 创建 AttributeSelection 对象
        AttributeSelection attributeSelection = new AttributeSelection();
        attributeSelection.setEvaluator(chi2);
        attributeSelection.setSearch(ranker);

        try {
            // 类别标签属性索引（最后一列）
            int classIndex = wekaInstances.numAttributes() - 1;
            wekaInstances.setClassIndex(classIndex);
            // 应用特征选择到数据集上
            attributeSelection.SelectAttributes(dataset);

            // 获取选中的特征的索引
            int[] selectedAttributes = attributeSelection.selectedAttributes();

            return getSubList(target, selectedAttributes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    // 将 OpenMapRealMatrix 转换为 Weka 的 Instances 对象
    private static Instances convertToWekaInstances(OpenMapRealMatrix featureMatrix, List<Integer> classLabels, List<String> target) throws Exception {
        // BlockRealMatrix denseMatrix = new BlockRealMatrix(featureMatrix.getData());
        // System.out.println(denseMatrix);
        int numRows = featureMatrix.getRowDimension();
        int numCols = featureMatrix.getColumnDimension();

        // 创建FastVector存储属性信息
        FastVector attributes = new FastVector();

        for (String att : target) {
            Attribute attribute = new Attribute(att);
            attributes.addElement(attribute);
        }

        // 创建 类别 的属性信息
        FastVector classValues = new FastVector();

        classValues.addElement("safe");
        classValues.addElement("flaky");

        Attribute classAttribute = new Attribute("Class", classValues);

        attributes.addElement(classAttribute);

        // 创建 Instances 对象
        Instances wekaInstances = new Instances("FeatureMatrix", attributes, 0);

        // 填充数据到 Instances 对象
        for (int i = 0; i < numRows; i++) {
            Instance instance = new SparseInstance(numCols + 1);

            for (int j = 0; j < numCols; j++) {
                instance.setValue((Attribute) attributes.elementAt(j), featureMatrix.getEntry(i, j));
            }

            instance.setValue(classAttribute, classLabels.get(i) == 0 ? "safe" : "flaky");
            wekaInstances.add(instance);
        }
        // 存储ARFF文件
        ArffSaver arffSaver = new ArffSaver();
        arffSaver.setInstances(wekaInstances);
        arffSaver.setFile(new File(ARFF_FILE_NAME));
        arffSaver.writeBatch();
        // wekaInstances = binData(wekaInstances);

        return wekaInstances;
    }

    // 数据分箱
    private static Instances binData(Instances wekaInstance) throws Exception {
        NumericToNominal filter = new NumericToNominal();
        filter.setInputFormat(wekaInstance);
        return Filter.useFilter(wekaInstance, filter);
    }


    public static <T> List<T> getSubList(List<T> objects, int[] indexes) {
        List<T> res = new ArrayList<>();
        for (int index : indexes) {
            if (objects.size() == index) {
                continue;
            }
            res.add(objects.get(index));
        }
        return res;
    }

    /**
     * @Description Computes the Y/label vectors were 1=flaky and 0=safe.
     * @Param [experiment, subSet]
     * experiment: Experiment object representing the current experiment set-up
     * subSet: list of dictionaries with keys=train/valid/test and values=subsets.
     * @Return dictionary with keys=train/valid/test and values=label vector
     * @Since version 1.0
     */
    private static Map<String, List<Integer>> yValues(Map<String, DataFrame> subSet) {
        Map<String, List<Integer>> y = new HashMap<>();
        subSet.forEach((key, value) -> {
            List<Integer> v = new ArrayList<>();
            for (Object flaky : value.getColumn("flaky")) {
                v.add(flaky.equals("flaky") ? 1 : 0);
            }
            y.put(key, v);
        });
        return y;
    }

    /**
     * @Description Vectorizes the subsets in 'sets' using the tfidf and kbest selection.
     * @Param [experiment, sets]
     * experiment: Experiment object representing the current experiment set-up
     * sets: list of dictionaries with keys=train/valid/test and values=subsets.
     * @Return VECTORS: dictionary with keys=train/valid/test and values=info_dictionary.
     * info_dictionary are dictionary with keys:
     * - X: the tfidf matrix
     * - y: the label vector
     * - info: list of dictionary with additional metrics (see paper)
     * - feat: list of features (the column names of the tfidf matrix)
     * @Since version 1.0
     */
    public static Map<String, Map<String, Object>> vectorization(Experiment experiment, Map<String, DataFrame> sets) throws Exception {
        TFIDFMatrix matrix = xValues(experiment, sets);
        Map<String, List<Integer>> y = yValues(sets);

        Map<String, Map<String, Object>> vectors = new HashMap<>();

        Map<String, OpenMapRealMatrix> matrixes = matrix.getTfidfMatrixes();

        for (String key : matrixes.keySet()) {
            vectors.put(key, new HashMap<String, Object>(){{
                put("X", matrixes.get(key));
                put("Y", y.get(key));
                put("info", sets.get(key).getColumn("info"));
                put("feat", matrix.getFeatureNames());
            }});
        }
        return vectors;
    }
}
