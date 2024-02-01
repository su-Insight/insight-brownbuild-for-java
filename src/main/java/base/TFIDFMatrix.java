package base;

import org.apache.commons.math3.linear.OpenMapRealMatrix;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/22 下午7:35
 * @Version: 1.0
 */
public class TFIDFMatrix implements Serializable {
    private List<String> featureNames;
    private Map<String, OpenMapRealMatrix> tfidfMatrixes;
    public TFIDFMatrix(List<String> featureNames, Map<String, OpenMapRealMatrix> tfidfMatrix){
        this.featureNames = featureNames;
        this.tfidfMatrixes = tfidfMatrix;
    }

    public void add(String name, OpenMapRealMatrix matrix){
        tfidfMatrixes.put(name, matrix);
    }

    public List<String> getFeatureNames() {
        return featureNames;
    }

    public OpenMapRealMatrix get(String matrixName) {
        return tfidfMatrixes.get(matrixName);
    }

    public Map<String, OpenMapRealMatrix> getTfidfMatrixes(){
        return tfidfMatrixes;
    }
    public void print(){
        for (String key : tfidfMatrixes.keySet()) {
            System.out.println(tfidfMatrixes.get(key));
        }
    }
}
