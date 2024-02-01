package preprocessing;

import base.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static base.DataFrame.*;
import static core.MainProcess.extraToNumList;
import static tool.DeepCopy.deepCopyMap;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/6 下午5:34
 * @Version: 1.0
 */
public class SubSets {
    public static DataFrame getWordCount(DataFrame data, List<Integer> ngram){
        if (ngram.size() == 1){
            data.addColumn("wordCount", data.getColumn("wordCountNgram_" + String.valueOf(ngram.get(0))));
            return data;
        }

        List<Map<String, Integer>> col = new ArrayList<>(data.getColumnData().size());
        for (int i = 0; i < data.getColumnData().size(); i++){
            col.add(new HashMap<>());
        }

        for (Integer item : ngram){
            List<Object> wordCount = data.getColumn("wordCountNgram_" + String.valueOf(item));
            Map<String, Integer> wordCounts = null;
            for (int i = 0; i < wordCount.size(); i++){
                wordCounts = (Map<String, Integer>) wordCount.get(i);
                col.get(i).putAll(wordCounts);
            }
        }

        data.addColumn("wordCount", new ArrayList<>(col));
        return data;
    }

    /**
     * @Description Computes the commit_since_flaky metric.
     * @Param [data]
     * @Return Vector of size res_nbr_row containing the commit_since_flaky metric.
     * @Since version 1.0
     */
    public static List<Integer> getSinceFlaky(DataFrame data){
        Map<Group, List<Row>> groupData = data.groupByColumns(Collections.singletonList("commitID"));

        Title title = new Title();
        title.addAll(Arrays.asList("commitID", "flaky", "date", "commitIDIndex"));
        Map<Group, Boolean> flaky =  groupContainsValue(data.getColumnNames(), groupData, "flaky", "flaky");
        Map<Group, List<Object>> dates = getGroupedColumn(data.getColumnNames(), groupData, "date");
        Map<Group, String> date = getGroupEarliestDate(dates);
        Map<Group, List<Integer>> ids = getGroupedIndex(data.getColumnData(), groupData);
        DataFrame commitSinceFlaky = new DataFrame(new Title(), new ArrayList<>());
        List<Object> commitID = getGroupsAsString(groupData);
        commitSinceFlaky.addColumn(title.get(0), commitID);
        commitSinceFlaky.addColumn(title.get(1), flaky.values().stream().map(aBoolean -> (Object) aBoolean).collect(Collectors.toList()));
        commitSinceFlaky.addColumn(title.get(2), date.values().stream().map(string -> (Object) string).collect(Collectors.toList()));
        commitSinceFlaky.addColumn(title.get(3), ids.values().stream().map(integers -> (Object) integers).collect(Collectors.toList()));
        commitSinceFlaky.sortByDate("date");

        List<Object> sinceFlaky = new ArrayList<>();
        AtomicInteger count = new AtomicInteger();
        commitSinceFlaky.getColumn("flaky").stream()
                .map(e -> (boolean)e)
                .forEach(boolValue -> {
                    sinceFlaky.add(count.getAndIncrement());
                    if (boolValue) {
                        count.set(0);
                    }
        });
        commitSinceFlaky.addColumn("sinceFlaky", sinceFlaky);

        List<Integer> res = new ArrayList<>(Collections.nCopies(data.getColumnData().size(), 0));
        for (int i = 0; i < commitSinceFlaky.getColumnData().size(); i++) {
            for (Integer id : extraToNumList(String.valueOf(commitSinceFlaky.getColumn("commitIDIndex").get(i)))){
                res.set(id, (Integer) commitSinceFlaky.getColumn("sinceFlaky").get(i));
            }
        }
        return res;
    }

    /**
     * @return
     * @Description Computes the 'info' column depending, which contains the other metrics (#rerun, #fail, #success, commit_since_flaky).
     * @Param [data]
     * @Return modified dataset in a pandas dataframe format, with added 'info' column.
     * @Since version 1.0
     */
    public static DataFrame getInfoRerun(DataFrame data){
        List<Map<String, Integer>> info = new ArrayList<>();
        for (int i = 0; i < data.getColumnData().size(); i++){
            info.add(new HashMap<>());
        }
        String currentCommitXJob = "";

        List<Integer> sinceFlaky = getSinceFlaky(data);

        data.sortByColumn("commitID");
        data.sortByColumn("jobName");
        data.sortByDate("date");

        List<Object> indexes = data.getColumn("index");
        int index = 0;
        int statusIndex = data.indexOfColumn("status");
        Map<String, Integer> currentDict = null;
        for (Row row : data.getColumnData()){
            int i = (Integer) indexes.get(index++);
            if (!currentCommitXJob.equals((String) row.get(data.indexOfColumn("commitID")) + row.get(data.indexOfColumn("jobName")))){
                currentCommitXJob = (String) row.get(data.indexOfColumn("commitID")) + row.get(data.indexOfColumn("jobName"));
                currentDict = new HashMap<String, Integer>(){{
                    put("rerun", 0);
                    put("fail", 0);
                    put("success", 0);
                    put("commit_since_flaky", 0);
                }};
            }
            currentDict.put("commit_since_flaky", sinceFlaky.get(i));
            info.set(i, deepCopyMap(currentDict));

            currentDict.merge("rerun", 1, Integer::sum);
            currentDict.merge("fail", (Integer) row.get(statusIndex), Integer::sum);
            currentDict.merge("success", 1 - (Integer) row.get(statusIndex), Integer::sum);
        }
        data.addColumn("info", new ArrayList<Object>(info));
        data.addColumn("commitSinceFlaky", new ArrayList<Object>(sinceFlaky));
        return data;
    }

    /**
     * @Description Masks the non-failure jobs form a set 'res'.
     * @Param [data, maskFailure]
     * data: list of dictionaries with keys=train/valid/test and values=subsets.
     * maskFailure :mask condition.
     *      If cond=='Train', mask non-failure jobs in the training and test set.
     *      If cond=='All'  , mask non-failure jobs in all sets.
     *      Else (cond=='null'), mask non-failure jobs in the test set.
     * @Return list of dictionaries with keys=train/valid/test and values=modified subsets according to the mask.
     * @Since version 1.0
     */
    public static Map<String, DataFrame> maskFailure(Map<String, DataFrame> sets, String maskFailure){
        List<String> mask = Collections.singletonList("test");
        if (maskFailure.equals("Train")) {
            mask = Arrays.asList("train", "test");
        }
        if (maskFailure.equals("All")) {
            mask = Arrays.asList("train", "valid", "test");
        }

        for (String con : mask){
            sets.put(con, justFailure(sets.get(con)));
        }
        return sets;
    }
    /**
     * @Description Masks the non-failure jobs form a set 'res'.
     * @Param full dataset in a DataFrame format.
     * @Return modified dataset in a pandas dataframe format, containing only failing jobs.
     * @Since version 1.0
     */
    public static DataFrame justFailure(DataFrame data){
        List<Object> status = data.getColumn("status");
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < status.size(); i++){
            if (status.get(i).equals(1)){
                indexes.add(i);
            }
        }

        if (indexes.isEmpty()) {
            throw new RuntimeException("Train/test/valid set is empty. (provide a sufficiently large dataset)");
        }

        return data.getLocationData(indexes);
    }
    public static List<Integer> extractIDsfromIDWanted(List<List<Integer>> idWanted, int startIndex, int endIndex){
        List<Integer> res = new ArrayList<>();
        for (int i = startIndex; i < endIndex; i++){
            res.addAll(idWanted.get(i));
        }
        return res;
    }
    /**
     * @Description Oversamples the sets to have the same ratio of failuresXflakiness in the sets.
     * @Param subset of DataFrame format.
     * @Return oversampled subset.
     * @Since version 1.0
     */
    public static DataFrame overSampling(DataFrame data){
        data.resetIndex();
        List<List<Integer>> idsStatusFlaky = new ArrayList<>();
        int currentIDsIndex = 0;
        int sortIndex = data.indexOfColumn("index");
        int statusIndex = data.indexOfColumn("status");
        int flakyIndex = data.indexOfColumn("flaky");
        for (Object status : new HashSet<>(data.getColumn("status"))){
            for (Object flaky : new HashSet<>(data.getColumn("flaky"))){
                for(Row row : data.getColumnData()){
                    if (row.get(statusIndex) == status && row.get(flakyIndex) == flaky) {
                        try {
                            idsStatusFlaky.get(currentIDsIndex).add((Integer) row.get(sortIndex));
                        } catch (IndexOutOfBoundsException e){
                            idsStatusFlaky.add(new ArrayList<>());
                            idsStatusFlaky.get(currentIDsIndex).add((Integer) row.get(sortIndex));
                        }
                    }
                }
            }
        }

        // 找到列表中最大元素的长度
        int maxLen = 0;
        for (List<Integer> ids : idsStatusFlaky) {
            maxLen = Math.max(maxLen, ids.size());
        }

        List<Integer> newIDs = new ArrayList<>();
        for (List<Integer> ids : idsStatusFlaky) {
            Collections.shuffle(ids);
            List<Integer> _ids = new ArrayList<>(ids);
            for (int i = 0; i < Math.ceil((double) maxLen / ids.size()); i++) {
                ids.addAll(_ids);
            }
            newIDs.addAll(ids.subList(0, maxLen));
        }
        Collections.sort(newIDs);
        return data.getLocationData(newIDs).resetIndex();
    }
    /**
     * @Description Generates subsets for train(90%)/valid(5%)/test(5%). The subsets selected
     *     jobs by commitID (all the jobs of a commitID will be in the same set).
     *     If want=True, the subsets generated will only contain jobs from commitID that
     *     have at least one flaky job. If want=False,  the subsets generated will only
     *     contain jobs from commitID that only has safe builds.
     * @Param [data, ids, flaky, want]
     * data: full dataset in a DataFrame format.
     * ids: list of list of ids, each sublist lists the line ids of jobs by commitID. (size: nbr_commitID).
     * flaky: list of boolean, indicating if the commitID (related to the 'ids') contains a flaky job or not.
     * want: boolean. If True, will only select commitIDs that contain at least a flaky job. If False, will only select
     *      commitIDs that contain only safe builds.
     * @Return java.lang.Object
     * sets: list of dictionaries with keys=train/valid/test and values=subsets.
     * @Since version 1.0
     */
    public static Map<String, DataFrame> randomSetsByType(DataFrame data, Map<Group, List<Integer>> ids, Map<Group, Boolean> flakys, boolean want){
        List<List<Integer>> idWanted = new ArrayList<>();
        flakys.forEach((group, flaky) -> {
            if (flaky == want) {
                idWanted.add(ids.get(group));
            }
        });
        Collections.shuffle(idWanted);

        int percent10Limit = Math.round(idWanted.size() * 0.05f);

        List<Integer> test = extractIDsfromIDWanted(idWanted , 0, percent10Limit);
        List<Integer> valid = extractIDsfromIDWanted(idWanted, percent10Limit, percent10Limit * 2);
        List<Integer> train = extractIDsfromIDWanted(idWanted, percent10Limit * 2, idWanted.size());

        Map<String, DataFrame> sets = new HashMap<>();
        sets.put("test", data.getLocationData(test));
        sets.put("valid", data.getLocationData(valid));
        sets.put("train", data.getLocationData(train));

        return sets;
    }
    public static DataFrame shuffle(DataFrame dataFrame){
        List<Integer> indexes = IntStream.rangeClosed(0, dataFrame.getColumnData().size() - 1)
                .boxed()
                .collect(Collectors.toList());
        Collections.shuffle(indexes);
        return dataFrame.getLocationData(indexes);
    }
    /**
     * @Description Generates subsets for train(90%)/valid(5%)/test(5%). The subsets selected jobs by commitID (all
     *      the jobs of a commitID will be in the same set). The ratio of commitID with flakiness and without is
     *      respected in the subsets.
     * @Param [data]
     * @Return sets: list of dictionaries with keys=train/valid/test and values=subsets.
     * @Since version 1.0
     */
    public static Map<String, DataFrame> randomSets(DataFrame data){
        data.resetIndex();
        Map<Group, List<Row>> groupedData = data.groupByColumns(Collections.singletonList("commitID"));
        Map<Group, List<Integer>> commitIDs = getGroupedIndex(data.getColumnData(), groupedData);
        Map<Group, Boolean> commitFlaky = groupContainsValue(data.getColumnNames(), groupedData, "flaky", "flaky");

        Map<String, DataFrame> flakys = randomSetsByType(data, commitIDs, commitFlaky, true);
        Map<String, DataFrame> safes = randomSetsByType(data, commitIDs, commitFlaky, false);

        Map<String, DataFrame> res = new HashMap<>();
        Set<String> keys = flakys.keySet();
        for (String key : keys){
            Title title = new Title();
            title.addAll(data.getColumnNames());
            DataFrame dataFrame = new DataFrame(title, new ArrayList<>());
            dataFrame.replaceDataWithShuffle(flakys.get(key).getColumnData(), safes.get(key).getColumnData());
            // dataFrame = shuffle(dataFrame).resetIndex();
            res.put(key, dataFrame.resetIndex());
        }
        return res;
    }
    /**
     * @Description Generates subsets for train(90%)/valid(5%)/test(5%), adds the necessary  columns (word_count and info),
     *      applies the mask failure and oversampling as indicated in the Experiment object 'P'.
     * @Param [experiment, data]
     * experiment: Experiment object representing the current experiment set-up.
     * data: full dataset in a DataFrame format.
     * @Return list of dictionaries with keys=train/valid/test and values=subsets.
     * @Since version 1.0
     */
    public static Map<String, DataFrame> subSets(Experiment experiment, DataFrame data){
        Map<String, DataFrame> sets = randomSets(data);
        for (String key : sets.keySet()) {
            sets.put(key, getWordCount(sets.get(key), experiment.ngram));
            sets.put(key, getInfoRerun(sets.get(key)));
        }
        maskFailure(sets, experiment.failMask);

        if (experiment.overSampling) {
            sets.put("train", overSampling(sets.get("train")));
        }

        return sets;
    }
}