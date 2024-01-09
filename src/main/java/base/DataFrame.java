package base;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/8 下午5:51
 * @Version: 1.0
 */
public class DataFrame {
    private Map<String, List<Object>> colData = new HashMap<>();
    private List<String> colNames;
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

    public Map<List<String>, Float> getGroupedAverage(Map<List<String>, List<List<Object>>> groupedData, String colName){
        Map<List<String>, Float> res = new HashMap<>();
        int index = colNames.indexOf(colName);
        groupedData.forEach((group, dataList) -> {
            Float average = (float)dataList.stream().mapToDouble(list -> ((Number) list.get(index)).doubleValue()).average().getAsDouble();
            res.put(group, average);
        });
        return res;
    }

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

}
