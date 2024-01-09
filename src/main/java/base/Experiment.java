package base;

import core.MainProcess;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/6 下午10:20
 * @Version: 1.0
 */
public class Experiment implements Serializable {
    public String pathData;
    public String settingName;
    public List<Integer> ngram;
    public Boolean overSampling;
    public String failMask;
    public int kbestThresh;
    public int alpha;
    public int beta;
    public String pathExp;
    public Experiment(String pathData, String settingName, List<Integer> ngram, Boolean overSampling, String failMask, int kbestThresh, int alpha, int beta){
        this.pathData = pathData;
        this.ngram = ngram;
        this.overSampling = overSampling;
        this.failMask = failMask;
        this.kbestThresh = kbestThresh;
        this.alpha = alpha;
        this.beta = beta;

        this.pathExp = MainProcess.pathExperiment + settingName + File.separator;
    }
}
