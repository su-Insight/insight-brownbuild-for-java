package core;

import org.apache.commons.cli.*;
import tool.Stemmer;
import tool.Timer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tool.File.getFileContent;
import static tool.File.getPathFiles;


/**
 * @author: insight
 * @description: todo
 * @date: 2023/12/31 下午2:17
 * @version: 1.0
 */
public class MainExtract {
    // MODE:
    // 0 => Verbose mode(default):show files' name and number processed;
    // 1 => Minimal mode:only show the process bar
    private static int mode = 0;
    private static long totalFile = 1;
    private static long completedFile = 0;
    private static int percentage = -1;
    private static final String fileRegex = ".*[\\/]((.*_.*_.*_.*_.*_.*)_(.*)_(.*)_([01])(_(.*))?)(-processed)?\\.log";
    private static final String[] stopWords = new String[]{"a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "amoungst", "amount", "an", "and", "another", "any", "anyhow", "anyone", "anything", "anyway", "anywhere", "are", "around", "as", "at", "back", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom", "but", "by", "call", "can", "cannot", "cant", "co", "con", "could", "couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight", "either", "eleven", "else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own", "part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thick", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", "the"};
    private static String pathIn = "";
    private static String pathOut = "";
    private static int proc = 2;
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
                        if(mode == 0){
                            System.out.println("Thread "+ Thread.currentThread().getName() +" has processed.");
                        }
                        latch.countDown();
                        break;
                    }
                    String fileName = files.take(); // 如果队列为空，会阻塞等待文件名
                    if (mode == 0){
                        System.out.print("File " + fileName + " is processing by thread: " + Thread.currentThread().getName());
                        System.out.print("   ");
                        System.out.print(++completedFile);
                        System.out.print("/");
                        System.out.println(totalFile);
                    }

                    StringBuffer content = getFileContent(fileName);
                    String con = "";
                    Pattern pattern = Pattern.compile(fileRegex);
                    Matcher matcher = pattern.matcher(fileName);
                    if (!matcher.find()){
                        throw new RuntimeException("dataset naming format error");
                    }

                    for(ApplicableRegex applicableRegex : applicableRegexes){
                        con = applicableRegex.regex.matcher(content.toString()).replaceAll(applicableRegex.replacement);
                    }

                    // 待处理文本
                    String[] processingWords = con.toLowerCase().split("\\s+");
                    List<String> words = Stemmer.batchStemmer(processingWords);
                    List<String> redWords = new ArrayList<>();
                    for (String word : words){
                        if (!contains(stopWords, word.toLowerCase()) && word.length() > 2){
                            redWords.add(word.toLowerCase());
                        }
                    }

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

                    if (mode == 1){
                        completedFile++;
                        percentage = upProgress(percentage, totalFile, percentage);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    public static Boolean contains(String[] strings, String string){
        return Arrays.asList(strings).contains(string);
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
    public synchronized static int upProgress(long completedFile, long totalFile, int percentage){
        if (percentage == (int) (completedFile * 100 / totalFile)){
            return percentage;
        }else {
            percentage = (int) (completedFile * 100 / totalFile);
            System.out.print("[");
            for (int i = 1; i <= percentage; i+=2) {
                System.out.print("█");
            }
            for (int i = 1; i < 100-percentage; i+=2) {
                System.out.print(" ");
            }
            System.out.print("]  " + percentage + "%   \r");
            if (percentage == 100) {
                return -1;
            }
            return percentage;
        }
    }

    public static void extractArgs(String[] args){
        Options options = new Options();
        options.addRequiredOption("proc", null, true, "Number of threads");

        options.addRequiredOption("path", null, true, "Input path containing log files.");
        options.addRequiredOption("out", null, true, "File path for the output of vocabulary extraction");
        options.addOption("m", "minimal", false, "Use Minimal mode");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);

            proc = Integer.parseInt(cmd.getOptionValue("proc"));
            pathIn = cmd.getOptionValue("path");
            pathOut = cmd.getOptionValue("out");
            mode = cmd.hasOption("m") ? 1 : 0;
        }catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar <jarName>", options);
            System.exit(1);
        }
    }

    public static void main(String[] args){
        extractArgs(args);

        totalFile = Objects.requireNonNull(new File(pathIn).list()).length;

        long start = System.currentTimeMillis();

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
            // 检查文件分隔符，防止拼接出现错误
            if (!pathIn.endsWith(File.separator)){
                pathIn += File.separator;
            }
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
            System.out.println("\n\nDone:" + pathOut);
            System.out.println("--- " + Timer.durationTimeFormat(System.currentTimeMillis() - start) + "--- ");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 关闭线程池
        executorService.shutdown();
    }


}
