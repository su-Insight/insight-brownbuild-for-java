package base;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/8 下午5:51
 * @Version: 1.0
 */
public class DataFrame implements Serializable {
    // store in columns
    private Map<String, List<Object>> colData = new HashMap<>();
    private List<String> colNames = new ArrayList<>();

    public DataFrame() {
    }

    public DataFrame(List<String> colNames, List<List<Object>> colDatas){
        this.colNames = colNames;
        for (String colName : colNames){
            this.colData.put(colName, new ArrayList<>());
        }
        for (List<Object> line : colDatas){
            for (int i = 0; i < colNames.size(); i++){
                this.colData.get(colNames.get(i)).add(line.get(i));
            }
        }
    }

    @Override
    public String toString() {
        return "DataFrame{" +
                "colData=" + colData.toString() +
                ", colNames=" + colNames.toString() +
                '}';
    }

    public Map<String, List<Object>> getAllColumnData(){
        return colData;
    }
    public List<Object> getColumn(String colName){
        if (!colData.containsKey(colName)){
            throw new RuntimeException("column is not found.");
        }
        return colData.get(colName);
    }
    public void addColumn(String colName, List<Object> colData){
        colNames.add(colName);
        this.colData.put(colName, colData);
    }

    /**
     * @Author Insight
     * @Date 2024/1/9 下午9:34
     * @Description Generate the data stored in row
     * @Param []
     * @Return java.util.List<java.util.List<java.lang.Object>>
     * @Since version 1.0
     */
    public List<List<Object>> revertData(){
        List<List<Object>> originalRowData = new ArrayList<>();
        AtomicInteger index = new AtomicInteger(0);
        AtomicInteger colIndex = new AtomicInteger(0);

        for (int i = 0; i < colData.get(colNames.get(0)).size(); i++){
            originalRowData.add(new ArrayList<>());
        }

        for (int i = 0; i < colData.size(); i++) {
            // 根据colNames顺序生成originalData，保证数据的一致性
            List<Object> data = colData.get(colNames.get(i));
            for (Object item : data){
                originalRowData.get(index.getAndIncrement()).add(item);
            }
            index.set(0);
            colIndex.getAndIncrement();
        }
        return originalRowData;
    }

    public List<Object> getGroupedColumn(Map<List<String>, List<List<Object>>> groupedData, String colName){
        List<Object> res = new ArrayList<>();
        int index = colNames.indexOf(colName);
        groupedData.forEach((group, dataList) -> {
            for (List<Object> data : dataList){
                res.add(data.get(index));
            }
        });
        return res;
    }

    public Map<List<String>, Boolean> groupContainsValue(Map<List<String>, List<List<Object>>> groupedData, String colName, String containStr){
        Map<List<String>, Boolean> res = new HashMap<>();
        int index = colNames.indexOf(colName);
        groupedData.forEach((group, data) -> {
            Boolean tag = false;
            for (List<Object> row : data){
                if (String.valueOf(row.get(index)).contains(containStr)) {
                    tag = true;
                    break;
                }
            }
            res.put(group, tag);
        });
        return res;
    }

    /**
     * @Author Insight
     * @Date 2024/1/9 下午9:36
     * @Description Convert data in groups into index in full dataset
     * @Param [groupedData]
     * @Return java.util.Map<java.util.List<java.lang.String>,java.util.List<java.lang.Integer>>
     * @Since version 1.0
     */
    public Map<List<String>, List<Integer>> getGroupedIndex(Map<List<String>, List<List<Object>>> groupedData){
        Map<List<String>, List<Integer>> groupedIndex = new HashMap<>();
        List<List<Object>> originalRowData = revertData();
        groupedData.forEach((group, data) -> {
            List<Integer> indexs = new ArrayList<>();
            for (List<Object> row : data){
                indexs.add(originalRowData.indexOf(row));
            }
            groupedIndex.put(group, indexs);
        });
        return groupedIndex;
    }

    /**
     * @Author Insight
     * @Date 2024/1/9 下午9:38
     * @Description Based on the grouping information, find the average number of a column in the group.
     * @Param [groupedData, colName]
     * @Return java.util.Map<java.util.List<java.lang.String>,java.lang.Float>
     * @Since version 1.0
     */
    public Map<List<String>, Float> getGroupedAverage(Map<List<String>, List<List<Object>>> groupedData, String colName){
        Map<List<String>, Float> res = new HashMap<>();
        int index = colNames.indexOf(colName);
        groupedData.forEach((group, dataList) -> {
            Float average = (float)dataList.stream().mapToDouble(list -> ((Number) list.get(index)).doubleValue()).average().getAsDouble();
            res.put(group, average);
        });
        return res;
    }

    /**
     * @Author Insight
     * @Date 2024/1/9 下午5:46
     * @Description This is description of method
     * @Param [groupByList, originalRowData]
     * @Return Data grouped by columns stored in row.
     * @Since version 1.0
     */
    public Map<List<String>, List<List<Object>>> groupByColumns(List<String> groupByList, List<List<Object>> originalRowData) {
        Map<List<String>, List<List<Object>>> groupedData = new HashMap<>();
        for (List<Object> row : originalRowData) {
            List<String> groupBys = new ArrayList<>();
            for (String groupBy : groupByList){
                groupBys.add(String.valueOf(row.get(colNames.indexOf(groupBy))));
            }

            if (groupedData.containsKey(groupBys)){
                groupedData.get(groupBys).add(row);
            }else {
                List<List<Object>> list = new ArrayList<>();
                list.add(row);
                groupedData.put(groupBys, list);
            }
        }
        return groupedData;
    }

    public Map<List<String>, List<List<Object>>> groupByColumns(List<String> groupByList){
        return groupByColumns(groupByList, revertData());
    }
}
