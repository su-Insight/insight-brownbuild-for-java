package base;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import preprocessing.GetData;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/8 下午5:51
 * @Version: 1.0
 */
public class DataFrame implements Serializable {
    // store in columns
    // private Map<String, List<Object>> colData = new HashMap<>();
    private List<Row> columnData;
    private Title columnNames = new Title();

    public DataFrame() {
    }

    public DataFrame(Title columnNames, List<Row> columnData){
        this.columnNames = columnNames;
        this.columnData = columnData;
    }

    public DataFrame resetIndex(){
        String indexColName = "index";
        int index;
        boolean isContain = columnNames.contains(indexColName);
        if (isContain) {
            index = indexOfColumn(indexColName);
        }else {
            index = 0;
            columnNames.add(index, indexColName);
        }
        columnData = IntStream.range(0, columnData.size())
                .mapToObj(i -> {
                    Row row = columnData.get(i);
                    if (isContain) {
                        row.set(index, i);
                    }else {
                        row.add(index, i);
                    }
                    return row;
                })
                .collect(Collectors.toList());
        return this;
    }

    public void print(){
        final int MAX_LENGTH = 60;
        int sum = 0;
        // 获取列数
        int columns = columnNames.size();

        // 计算每列最大字符数
        int[] columnWidths = new int[columns];
        for (Row row : columnData) {
            int s = 0;
            for (int i = 0; i < columns; i++) {
                int length = Math.max(String.valueOf(row.get(i)).length(), columnNames.get(i).length());
                columnWidths[i] = Math.min(Math.max(columnWidths[i], length), MAX_LENGTH);
                s += (columnWidths[i] + 2);
            }
            sum = Math.max(s, sum);
        }
        System.out.println(String.join("", Collections.nCopies(sum, "\u2500")));

        // 打印表头
        for (int i = 0; i < columns; i++) {
            System.out.printf("%-" + (columnWidths[i] + 2) + "s", columnNames.get(i));
        }
        System.out.println();
        System.out.println(String.join("", Collections.nCopies(sum, "-")));

        // 打印数据
        for (int i = 0; i < columnData.size(); i++) {
            for (int j = 0; j < columns; j++) {
                String cellValue = String.valueOf(columnData.get(i).get(j));
                int length = Math.min(MAX_LENGTH, cellValue.length());

                if (length == MAX_LENGTH) {
                    // If data length exceeds 30, display the first 10, "..." and the last 10 characters
                    String truncatedValue = cellValue.substring(0, MAX_LENGTH / 2 - 3) + "..." + cellValue.substring(cellValue.length() - MAX_LENGTH / 2 + 3);
                    System.out.printf("%-" + (columnWidths[j] + 2) + "s", truncatedValue);
                } else {
                    System.out.printf("%-" + (columnWidths[j] + 2) + "s", cellValue);
                }
            }
            System.out.println();
        }
        System.out.println(String.join("", Collections.nCopies(sum, "-")));
        System.out.println(columnData.size() + " rows x " + columnNames.size() + " columns");
        System.out.println(String.join("", Collections.nCopies(sum, "\u2500")));
    }
    public void printCompat() {
        // 使用StringWriter创建CSV格式化输出
        try {
            StringWriter stringWriter = new StringWriter();
            CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.DEFAULT);

            csvPrinter.printRecord(columnNames);
            // 将数据写入CSV格式
            for (List<Object> row : columnData) {
                csvPrinter.printRecord(row);
            }

            // 获取CSV格式化输出的字符串
            String csvOutput = stringWriter.toString();

            // 打印结果
            System.out.println(csvOutput);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Integer indexOfColumn(String colName){
        return columnNames.indexOf(colName);
    }

    public List<Row> getColumnData(){
        return columnData;
    }
    public Title getColumnNames(){
        return columnNames;
    }
    public List<Object> getColumn(String colName){
        int index = indexOfColumn(colName);
        List<Object> res = new ArrayList<>();
        columnData.forEach((row) -> {
            res.add(row.get(index));
        });
        return res;
    }
    public void addColumn(String colName, List<Object> colData){
        int index = indexOfColumn(colName);
        addColumn(colName, colData, index == -1 ? columnNames.size() : index);
    }
    public void addColumn(String colName, List<Object> colData, int index) {
        columnNames.add(index, colName);
        final boolean IS_NULL = columnData.isEmpty();
        for (int i = 0; i < colData.size(); i++){
            if (IS_NULL) columnData.add(new Row());
            columnData.get(i).add(index, colData.get(i));
        }
    }

    public void sortByDate(String dateColName){
        int index = indexOfColumn(dateColName);
        columnData = columnData.stream().sorted(Comparator.comparing(row -> {
            try {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse((String) row.get(index));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        })).collect(Collectors.toList());
    }

    public void sortByColumn(String colName){
        int index = indexOfColumn(colName);
        columnData.sort(Comparator.comparing(row -> row.get(index).toString()));
    }

    public void mergeDataWithShuffle(List<Row> res1, List<Row> res2){
        List<Row> res = new ArrayList<>();
        res.addAll(res1);
        res.addAll(res2);

        Collections.shuffle(res);

        this.columnData = res;
    }


    public static Map<Group, List<Object>> getGroupedColumn(Title colNames, Map<Group, List<Row>> groupedData, String colName){
        Map<Group, List<Object>> res = new HashMap<>();
        int index = colNames.indexOf(colName);
        groupedData.forEach((group, dataList) -> {
            List<Object> values = new ArrayList<>();
            for (Row data : dataList){
                values.add(data.get(index));
            }
            res.put(group, values);
        });
        return res;
    }

    public static Map<Group, Boolean> groupContainsValue(Title colNames, Map<Group, List<Row>> groupedData, String colName, String containStr){
        Map<Group, Boolean> res = new HashMap<>();
        int index = colNames.indexOf(colName);
        groupedData.forEach((group, data) -> {
            boolean tag = false;
            for (Row row : data){
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
    public static Map<Group, List<Integer>> getGroupedIndex(List<Row> colData, Map<Group, List<Row>> groupedData){
        Map<Group, List<Integer>> groupedIndex = new HashMap<>();
        groupedData.forEach((group, data) -> {
            List<Integer> indexs = new ArrayList<>();
            for (Row row : data){
                indexs.add(colData.indexOf(row));
            }
            groupedIndex.put(group, indexs);
        });
        return groupedIndex;
    }

    public static Map<Group, String> getGroupEarliestDate(Map<Group, List<Object>> allDate){
        Map<Group, String> res = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<Date> dateObjects = new ArrayList<>();
        allDate.forEach((group, dates) -> {
            for (Object dateString : dates) {
                try {
                    Date date = dateFormat.parse((String) dateString);
                    dateObjects.add(date);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
            Date earliestDate = Collections.min(dateObjects);
            dateObjects.clear();
            res.put(group, dateFormat.format(earliestDate));
        });
        return res;
    }

    public static List<Object> getGroupsAsString(Map<Group, List<Row>> groupedData){
        List<Object> res = new ArrayList<>();
        groupedData.forEach((group, rows) -> {
            String gr = "";
            for (Object g : group){
                gr += gr.isEmpty() ? g.toString() : ("_" + g.toString());
            }
            res.add(gr);
        });
        return res;
    }

    /**
     * @Author Insight
     * @Date 2024/1/9 下午9:38
     * @Description Based on the grouping information, find the average number of a column in the group.
     * @Param [groupedData, colName]
     * @Return java.util.Map<java.util.List<java.lang.String>,java.lang.Float>
     * @Since version 1.0
     */
    public static Map<Group, Float> getGroupedAverage(Title colNames, Map<Group, List<Row>> groupedData, String colName){
        Map<Group, Float> res = new HashMap<>();
        int index = colNames.indexOf(colName);
        groupedData.forEach((group, dataList) -> {
            Float average = (float)dataList.stream().mapToDouble(list -> (Double.parseDouble(String.valueOf(list.get(index))))).average().getAsDouble();
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
    public Map<Group, List<Row>> groupByColumns(List<String> groupByList) {
        Map<Group, List<Row>> groupedData = new HashMap<>();
        for (Row row : columnData) {
            Group groupBys = new Group();
            for (String groupBy : groupByList){
                groupBys.add(String.valueOf(row.get(indexOfColumn(groupBy))));
            }

            if (groupedData.containsKey(groupBys)){
                groupedData.get(groupBys).add(row);
            }else {
                groupedData.put(groupBys, new ArrayList<>(Collections.singleton(row)));
            }
        }
        return groupedData;
    }

    public DataFrame getLocationData(List<Integer> indexes){
        DataFrame res = new DataFrame(columnNames, new ArrayList<>());
        for (Integer index : indexes){
            if (index < 0 || index > columnData.size()){
                throw new ArrayIndexOutOfBoundsException();
            }
            res.columnData.add(columnData.get(index));
        }
        return res;
    }
}
