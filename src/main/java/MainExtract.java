import org.apache.commons.lang.time.StopWatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.time.DurationFormatUtils.formatDuration;


/**
 * @author: insight
 * @description: todo
 * @date: 2023/12/31 下午2:17
 * @version: 1.0
 */
public class MainExtract {
    public static final String fileRegex = ".*[\\/]((.*_.*_.*_.*_.*_.*)_(.*)_(.*)_([01])(_(.*))?)(-processed)?\\.log";
    public static final String[] stopWords = new String[]{"a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "amoungst", "amount", "an", "and", "another", "any", "anyhow", "anyone", "anything", "anyway", "anywhere", "are", "around", "as", "at", "back", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom", "but", "by", "call", "can", "cannot", "cant", "co", "con", "could", "couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight", "either", "eleven", "else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own", "part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thick", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", "the"};

    static class ApplicableRegex{
        Pattern regex;
        String replacement;

        public ApplicableRegex(Pattern regex, String replacement){
            this.regex = regex;
            this.replacement = replacement;
        }
    }

    static class ProcessFilesTask implements Runnable {
        private final BlockingQueue<String> files;
        private final CountDownLatch latch;
        private final List<ApplicableRegex> applicableRegexes;
        private final String pathOut;

        public ProcessFilesTask(CountDownLatch latch, BlockingQueue<String> files, List<ApplicableRegex> applicableRegexes, String pathOut) {
            this.latch = latch;
            this.files = files;
            this.applicableRegexes = applicableRegexes;
            this.pathOut = pathOut;
        }

        @Override
        public void run() {
            while (true){
                try {
                    // 从阻塞队列中获取文件名
                    if (files.isEmpty()){
                        System.out.println("Thread "+ Thread.currentThread().getName() +" has processed.");
                        // 处理完成后，调用 countDown() 方法来减少计数
                        latch.countDown();
                        break;
                    }
                    String fileName = files.take(); // 如果队列为空，会阻塞等待文件名
                    // 处理文件的操作
                    System.out.println("File " + fileName + " is processing by thread: " + Thread.currentThread().getName());
                    String content = getFileContent(fileName);

                    Pattern pattern = Pattern.compile(fileRegex);
                    Matcher matcher = pattern.matcher(fileName);
                    if (!matcher.find()){
                        throw new RuntimeException("dataset naming format error");
                    }

                    for(ApplicableRegex applicableRegex : applicableRegexes){
                        content = applicableRegex.regex.matcher(content).replaceAll(applicableRegex.replacement);
                    }

                    // 待处理文本
                    String[] processingWords = content.toLowerCase().split("\\s+");
                    List<String> words = stemmer(processingWords);
                    List<String> redWords = new ArrayList<>();
                    for (String word : words){
                        if (!contains(stopWords, word.toLowerCase()) && word.length() > 2){
                            redWords.add(word.toLowerCase());
                        }
                    }

                    ArrayList<String> hot = new ArrayList<>(Arrays.asList(new String[]{"remov", "distdir", "skip", "length"}));

                    int ngram = 2;
                    String newContent = wordCount(redWords, ngram);
                    Path path = Paths.get(pathOut);
                    if (!Files.exists(path)) {
                        Files.createDirectories(path);
                    }

                    Path writePath = Paths.get(pathOut, matcher.group(1) + "-processed.csv");

                    Files.write(writePath, newContent.getBytes());
                    // 设置文件权限为可读、可写、可执行
                    Set<PosixFilePermission> permissions = new HashSet<>();
                    permissions.add(PosixFilePermission.OWNER_READ);
                    permissions.add(PosixFilePermission.OWNER_WRITE);
                    permissions.add(PosixFilePermission.OWNER_EXECUTE);
                    Files.setPosixFilePermissions(writePath, permissions);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    public static Boolean contains(String[] strings, String string){
        return Arrays.asList(strings).contains(string);
    }

    public static List<String> getPathFiles(String pathIn){
        return Arrays.asList(Objects.requireNonNull(new File(pathIn).list()));
    }

    public static String durationTimeFormat(long durationMills){
        return formatDuration(durationMills, "H'h'mm'm'ss.SSS's'");
    }
    public static String getFileContent(String filePath){
        StringBuilder content = new StringBuilder();
        try {
            // 创建一个 BufferedReader 对象来读取文件
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            // 读取文件内容并存储到字符串中
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n"); // 逐行添加到StringBuilder
            }
            // 关闭文件流
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return content.toString();
    }

    public static List<String> stemmer(String[] processingWords) throws IOException {
        List<String> words = new ArrayList<>();
        for (String word : processingWords) {
            words.add(Stemmer.stemmer(word));
        }
        return words;
    }

    public static String wordCount2String(Map<String, Integer> wordMap){
        StringBuilder res = new StringBuilder();
        wordMap.forEach((key, value) -> res.append(key).append(",").append(value).append("\n"));
        return res.toString();
    }

    public static String interWordCount(List<String> words){
        Map<String, Integer> counts = new HashMap<>(words.size());
        for (String word : words){
            word = word.toLowerCase();
            if(counts.putIfAbsent(word, 1) != null){
                counts.put(word, counts.get(word) + 1);
            }
        }
        return wordCount2String(counts);
    }

    public static String wordCount(List<String> words, int ngram){
        ArrayList<String> memoryWords = new ArrayList<>(words);

        String retString = interWordCount(memoryWords);
        List<String> wordsNgram = memoryWords;
        // 最多运行min(ngram, words.size() + 1)次
        for (int i = 1; i < ngram; i++){
            if (words.size() < i){
                return retString;
            }
            for (int j = i; j < words.size(); j++){
                wordsNgram.set(j-i, wordsNgram.get(j-i) + "_" + words.get(j));
            }
            retString += "#####" + interWordCount(memoryWords.subList(0, memoryWords.size() - i));
        }
        return retString;
    }
    public static void main(String[] args){
        if (args.length < 3){
            throw new IllegalArgumentException("the command line parameter format is incorrect");
        }
        int proc = Integer.parseInt(args[0]);
        String pathIn = args[1];
        String pathOut = args[2];

        if (!pathIn.endsWith(File.separator)){
            pathIn += File.separator;
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String[] regexes = new String[]{"\n", " ", "https?://[^\\s]+", "hypothesisurlforge", "[^\\s]+[/\\\\][^\\s]+", "hypothesispathforge", "[^\\s]+\\.[^\\s]+", "hypothesispathforge", "[\\d\\w]*\\w\\d[\\d\\w]*", "hypothesisnumletforge", "[\\d\\w]+\\d\\w[\\d\\w]*", "hypothesisnumletforge", "[_\\W]+", " ", "([A-Z]+)", " $1"};

        List<ApplicableRegex> applicableRegexes = new ArrayList<>();
        for(int i = 0; i < regexes.length; i+=2){
            applicableRegexes.add(new ApplicableRegex(Pattern.compile(regexes[i]), regexes[i+1]));
        }

        // 创建一个 CountDownLatch，用于等待所有线程完成
        CountDownLatch latch = new CountDownLatch(proc);
        // 创建一个阻塞队列，用于传递文件名
        BlockingQueue<String> files = new LinkedBlockingQueue<>();
        // 创建一个线程池
        ExecutorService executorService = Executors.newFixedThreadPool(proc);
        List<String> filesOnDisk = getPathFiles(pathIn);

        try {
            // 将文件名放入阻塞队列中
            for (String fileName : filesOnDisk) {
                files.put(pathIn + fileName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 启动多个线程处理文件
        for (int i = 0; i < proc; i++) {
            executorService.submit(new ProcessFilesTask(latch, files, applicableRegexes, pathOut));
        }




        try {
            // 等待所有线程完成
            latch.await();
            stopWatch.stop();
            System.out.println("Done:"+pathOut);
            System.out.println("--- " + durationTimeFormat(stopWatch.getTime()) + "--- ");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 关闭线程池
        executorService.shutdown();
    }


}
