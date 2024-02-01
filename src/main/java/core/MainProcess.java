package core;

import base.DataFrame;
import base.Experiment;
import ml.dmlc.xgboost4j.java.XGBoostError;
import preprocessing.GetData;
import preprocessing.SubSets;
import preprocessing.Vectorization;
import tool.PickCall;
import org.apache.commons.cli.*;
import tool.File;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static classification.Baseline.baseline;
import static classification.ClassificationXGBoost.classifyXGBoost;
import static tool.PickCall.runAndSerialize;
import static tool.Timer.durationTimeFormat;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/3 下午7:45
 * @Version: 1.0
 */
public class MainProcess {

    public static final String pathExperiment = "experiment/";
    private static boolean _10fold;
    private static boolean recompute;
    private static int mode = 0;

    static {
        // pathExperiment is the name of the folder that will contain the pickles of the experiments.
        File.checkAndMkdir(pathExperiment);
    }

    public static void main(String[] args) throws Exception {
        // Experiment experiment = extractArgs(args);
        Experiment experiment = new Experiment("/home/insight/Projects/gitSpace/insight-brownbuild-for-java/dataset/graphviz_extracted/", "default", new ArrayList<>(Arrays.asList(2)), true, "Train", 300, 70, 10);
        recompute = true;
        crossValidation(experiment);
    }

    public static Experiment extractArgs(String[] args){
        Options options = new Options();

        options.addRequiredOption("d", "path_data", true, "Path to the extracted dataset");
        options.addOption( "s", "setting_name", true, "Path to save the pickle files (Default= 'default')");
        options.addOption("n","ngram", true, "List of values N to consider (only values 1 and 2 are available with this extraction set-up) (Default= [2])");
        options.addOption("o", "oversampling", true, "If the training set is oversampled (Default= True)");
        options.addOption("f", "fail_mask", true, "Indicates which mask to apply (possible values: Train, None or All) (Default= 'Train')");
        options.addOption( "k", "kbest_thresh", true, "K value for the K best feature selection (Default= 300)");
        options.addOption("a", "alpha", true, "Int value (between 0 and 100, multiples of 10). Weight of model 1 in prediction (and 100-alpha is weight of model 2) (Default= 70)");
        options.addOption("b", "beta", true, "Int value (between 10 and 90, multiples of 10). Threshold for prediction brown (Default= 10)");
        options.addOption("10", "10fold", false, "If in the command, does the 10fold cross validation. If not, does simple cross validation");
        options.addOption("r", "recompute", false, "In in the command, does not use the previously computed pickles, recomputes everything");
        options.addOption("m", "minimal", false, "Use Minimal mode");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
            // 获取选项值
            String pathData = cmd.getOptionValue("path_data");
            String settingName = cmd.getOptionValue("setting_name") == null ? "default" : cmd.getOptionValue("setting_name");
            List<Integer> ngram = cmd.getOptionValue("ngram") == null ? new ArrayList<>(Arrays.asList(2)) : extraToNumList(cmd.getOptionValue("ngram"));
            Boolean overSampling = cmd.getOptionValue("oversampling") == null || Boolean.parseBoolean(cmd.getOptionValue("oversampling"));
            String failMask = cmd.getOptionValue("failMask") == null ? "Train" : cmd.getOptionValue("failMask");
            int kbestThresh = cmd.getOptionValue("kbestThresh") == null ? 300 : Integer.parseInt(cmd.getOptionValue("kbestThresh"));
            int alpha = cmd.getOptionValue("alpha") == null ? 70 : Integer.parseInt(cmd.getOptionValue("alpha"));
            int beta = cmd.getOptionValue("beta") == null ? 10 : Integer.parseInt(cmd.getOptionValue("beta"));
            _10fold = cmd.hasOption("10fold");
            recompute = cmd.hasOption("recompute");
            mode = cmd.hasOption("m") ? 1 : 0;

            return new Experiment(pathData, settingName, ngram, overSampling, failMask, kbestThresh, alpha, beta);

        }catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar <jarName>", options);
            System.exit(1);
        }
        return null;
    }
    public static List<Integer> extraToNumList(String strNums){
        String cleanedStr = strNums.replaceAll("\\[|\\]|\\s", "");
        String[] numbers = cleanedStr.split(",");

        List<Integer> res = new ArrayList<>();
        for (String num : numbers){
            res.add(Integer.parseInt(num));
        }
        return res;
    }
    public static void crossValidation(Experiment experiment) throws Exception {
        long start = System.currentTimeMillis();

        Object[] args = new Object[]{experiment.pathData};
        DataFrame data = runAndSerialize(GetData.class, GetData.class.getMethod("getData", String.class), args, experiment.pathExp + "data.p", recompute);

        args = new Object[]{experiment, data};
        Map<String, DataFrame> sets = runAndSerialize(SubSets.class, SubSets.class.getMethod("subSets", Experiment.class, DataFrame.class), args, experiment.pathExp + "sets.p", recompute);

        args = new Object[]{experiment, sets};
        Map<String, Map<String, Object>> vectors = runAndSerialize(Vectorization.class, Vectorization.class.getMethod("vectorization", Experiment.class, Map.class), args, experiment.pathExp + "vectors.p", recompute);

        Map<String, Map<String, Double>> baselines = baseline(data);
        Map<String, Map<String, Object>> big = classifyXGBoost(vectors);
        String id = String.format("%.1fvar_%dtresh", (float) experiment.beta, experiment.alpha);
        Map<String, Double> interest = (Map<String, Double>) big.get(id).get("result");

        printResults(baselines, interest);
        System.out.println("===== TOTAL TIME: " + durationTimeFormat(System.currentTimeMillis() - start) + " =====");
    }
    private static void printResults(Map<String, Map<String, Double>> baselines, Map<String, Double> interest) {
        String[] want = Arrays.asList("f1", "precision", "recall", "specificity").toArray(new String[0]);

        Object[] list = Arrays.asList("Run", "F1-Score", "Precision", "Recall", "Specificity").toArray();
        System.out.println(String.format("%-12s | %-12s %-12s %-12s %-12s |", list));
        System.out.println(String.join("", Collections.nCopies(68, "-")));

        for (String baseKey : baselines.keySet()) {
            list[0] = baseKey.toUpperCase();
            for (int i = 0; i < want.length; i++) {
                list[i+1] = String.valueOf(Math.round(100 * baselines.get(baseKey).get(want[i])));
            }
            System.out.println(String.format("%-12s | %-12s %-12s %-12s %-12s |", list));
        }

        list[0] = "XGB";
        for (int i = 0; i < want.length; i++) {
            list[i+1] = String.valueOf(Math.round(100 * interest.get(want[i])));
        }
        System.out.println(String.format("%-12s | %-12s %-12s %-12s %-12s |", list));
    }
    public static void crossValidation_10(Experiment experiment){
        long start = System.currentTimeMillis();
        System.out.println("run crossValidation");
        System.out.println("\n\nDone:" + experiment.pathData);
        System.out.println("--- " + durationTimeFormat(System.currentTimeMillis() - start) + "--- ");
    }


}
