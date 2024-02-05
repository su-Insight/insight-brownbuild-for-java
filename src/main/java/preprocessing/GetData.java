package preprocessing;

import base.DataFrame;
import base.Group;
import base.Row;
import base.Title;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import scala.tools.nsc.doc.html.page.JSONObject;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

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

        Map<Integer, Map<String, Integer>> dic = new HashMap<>(Math.min(separateText.size(), MAX_NGRAM));
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
    public static void getLogData(List<String> fileNames, String pathData) throws ParseException, IOException {
        Pattern pattern =  Pattern.compile(fileRegex);
        FileWriter writer = new FileWriter("data.csv");
        long total = fileNames.size();

        String[] header = new String[6 + MAX_NGRAM];
        int k = 0;
        for (String name : Arrays.asList("date", "jobID", "commitID", "status", "jobName", "filename")) {
            header[k++] = name;
        }
        for (int i = 1; i <= MAX_NGRAM; i++){
            header[k++] = "wordCountNgram_" + i;
        };

        k = 0;
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(header));
        Matcher matcher;
        for (String file : fileNames){
            matcher = pattern.matcher(file);
            if (matcher.find()){
                Map<Integer, Map<String, Integer>> word_count = getTextCount(pathData + file);
                Date date = new SimpleDateFormat(dateRegex).parse(matcher.group(2));
                csvPrinter.printRecord(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date),
                        matcher.group(3), matcher.group(4), matcher.group(5), matcher.group(7),
                        pathData + file, word_count.get(0), word_count.get(1));
                // date jobID commitID status jobName wordCount(wordCountNgram_1 wordCountNgram_2)
                percentage = upProgress(++k, total, percentage, 0, 50);
            }
        }
        csvPrinter.flush();
        csvPrinter.close();
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
            indexes.add(colNames.indexOf(column));
        }

        List<Object> extractedList = null;
        int k = 0;
        for (Row row : dataFrame.getColumnData()){
            extractedList = indexes.stream()
                    .map(row::get) // 索引从1开始
                    .collect(Collectors.toList());
            allFlakyStateRes.add(computeJobFlakyState.get(extractedList));
            // percentage = upProgress(k++, dataFrame.getColumnData().size(), percentage, 66, 100);
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
    public static DataFrame getData(String pathData) throws ParseException, IOException {
        List<String> logFiles = getPathFiles(pathData).stream().sorted().collect(Collectors.toList());

        getLogData(logFiles, pathData);

        Reader in = new FileReader("data.csv");
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
        Title colNames = new Title();
        colNames.addAll(records.iterator().next().toList());

        AtomicInteger ind = new AtomicInteger();
        List<Row> res = StreamSupport.stream(records.spliterator(), false)
                .map(record -> {
                    percentage = upProgress(ind.getAndIncrement(), logFiles.size(), percentage, 50, 100);
                    return new Row(
                            IntStream.range(0, colNames.size())
                                    .mapToObj(record::get)
                                    .collect(Collectors.toList())
                    );
                })
                .collect(Collectors.toList());

        DataFrame data = new DataFrame(colNames, res);
        flakyStateAll(data, colNames);
        return data.resetIndex();
    }

}
