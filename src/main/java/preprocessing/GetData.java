package preprocessing;

import base.DataFrame;
import base.Group;
import base.Row;
import base.Title;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static base.DataFrame.getGroupedAverage;
import static core.MainExtract.upProgress;
import static tool.File.getFileContent;
import static tool.File.getPathFiles;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/6 下午5:34
 * @Version: 1.0
 */
public class GetData {
    private static int MAX_NGRAM = 2;
    private static String fileRegex = "((.*_.*_.*_.*_.*_.*)_(.*)_(.*)_([01])(_(.*))?)-processed\\.csv";
    private static String dateRegex = "yyyy_MM_dd_HH_mm_ss";
    private static int percentage = -1;
    /**
     * @Author Insight
     * @Date 2024/1/7 下午5:44
     * @Description Get the word count in the file with filename 'file'.
     * @Param [filePath]
     * @Return The function returns a list of dictionary of word count for words generated with ngram where N in 1..MAX_NGRAM.
     * @Since version 1.0
     */
    public static Map<Integer, Map<String, Integer>> getTextCount(String filePath){
        StringBuffer text = getFileContent(filePath);
        String[] texts = text.toString().split("#");
        List<String> separateText = new ArrayList<>();
        for (String txt : texts){
            if (!"".equals(txt)){
                separateText.add(txt);
            }
        }

        separateText = separateText.subList(0, Math.min(separateText.size(), MAX_NGRAM));

        Map<Integer, Map<String, Integer>> dic = new HashMap<>(MAX_NGRAM);
        int count = 0;
        for (String txt : separateText){
            Map<String, Integer> loc = new HashMap<>();
            for (String line : txt.split("\n")){
                String[] rows = line.split(",");
                if (rows.length == 2 && rows[0].length() > 2){
                    loc.put(rows[0], Integer.parseInt(rows[1]));
                }
            }
            dic.put(count++, loc);
        }
        return dic;
    }
    /**
     * @Author Insight 
     * @Date 2024/1/9 上午12:12
     * @Description This is description of method
     * @Param [fileName, pathData]
     * @Return Returns a list representation of the job given in the file with filename 'file' at the path 'pathData'.
     * @Since version 1.0
     */
    public static Row getLogData(String fileName, String pathData) throws ParseException {
        Pattern pattern = Pattern.compile(fileRegex);
        Matcher matcher = pattern.matcher(fileName);
        Row loc = new Row();
        if (matcher.find()){
            Map<Integer, Map<String, Integer>> word_count = getTextCount(pathData + fileName);
            Date date = new SimpleDateFormat(dateRegex).parse(matcher.group(2));
            loc.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));    // date
            loc.add(matcher.group(3));  // jobID
            loc.add(matcher.group(4));  // commitID
            loc.add(Integer.parseInt(matcher.group(5)));  // status
            loc.add(matcher.group(7));  // jobName
            loc.add(pathData + fileName);
            for (int i = 0; i < word_count.size(); i++){
                loc.add(word_count.get(i));
            }
            return loc;
        }
        return null;
    }
    /**
     * @Author Insight 
     * @Date 2024/1/7 下午6:00
     * @Description This is description of method
     * @Param [mean]
     * "mean" mean result of the jobs run.
     * @Return Returns if a job is flaky or safe.
     * @Since version 1.0
     */
    public static String flakyState(float mean){
        return mean > 0 && mean < 1 ? "flaky" : "safe";     // unsteady results => flaky
    }
    /**
     * @Author Insight
     * @Date 2024/1/9 上午12:13
     * @Description Computes the flaky column.
     * @Param [dataFrame, originalData, colNames]
     * dataFrame: dataset in a dataframe format stored via column.
     * @Return modified dataset in a dataframe format, with added 'flaky' column.
     * @Since version 1.0
     */
    public static void flakyStateAll(DataFrame dataFrame, List<String> colNames){
        List<String> groupByList = Arrays.asList("commitID", "jobName");

        Map<Group, List<Row>> groupedData = dataFrame.groupByColumns(groupByList);
        Map<Group, Float> computeJobMeanStatus = getGroupedAverage(dataFrame.getColumnNames(), groupedData, "status");

        Map<Group, String> computeJobFlakyState = new HashMap<>();
        computeJobMeanStatus.forEach((group, mean) -> {
            computeJobFlakyState.put(group, flakyState(mean));
        });

        List<Object> allFlakyStateRes = new ArrayList<>();

        List<Integer> indexes = new ArrayList<>(groupByList.size());
        for (String column : groupByList){
            indexes.add(colNames.indexOf(column) + 1);
        }

        List<Object> extractedList = null;
        for (Row row : dataFrame.getColumnData()){
            extractedList = indexes.stream()
                    .map(position -> row.get(position - 1)) // 索引从1开始
                    .collect(Collectors.toList());
            allFlakyStateRes.add(computeJobFlakyState.get(extractedList));
        }

        dataFrame.addColumn("flaky", allFlakyStateRes);
    }
    /**
     * @Author Insight 
     * @Date 2024/1/9 上午12:15
     * @Description Gets data for path 'pathData'
     * @Param [pathData]
     * @Return dataset in a pandas dataframe format.
     * @Since version 1.0
     */
    public static DataFrame getData(String pathData) throws ParseException {
        List<Row> res = new ArrayList<>();
        List<String> logFiles = getPathFiles(pathData).stream().sorted().collect(Collectors.toList());
        Pattern pattern = Pattern.compile(fileRegex);
        long total = logFiles.size();
        int index = 0;
        for (String file : logFiles){
            if (pattern.matcher(file).find()){
                percentage = upProgress(++index, total, percentage);
                Row line = getLogData(file, pathData);
                res.add(line);
            }
        }

        Title colNames = new Title();
        colNames.addAll(Arrays.asList("date", "jobID", "commitID", "status", "jobName", "filename"));
        for (int i = 1; i <= MAX_NGRAM; i++){
            colNames.add("wordCountNgram_" + i);
        }
        DataFrame dataFrame = new DataFrame(colNames, res);
        flakyStateAll(dataFrame, colNames);
        dataFrame.resetIndex();
        return dataFrame;
    }

}
