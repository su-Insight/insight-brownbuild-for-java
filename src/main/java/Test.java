
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/1 下午12:17
 * @Version: 1.0
 */
public class Test {

    public static void main(String[] args) throws InterruptedException, IOException {
        // String[] s = "graphviz/2018_07_25_03_14_38_84151203_40b3c20f7e839725ae8d21531de347c2db2c1a2b_1_centos6build.log".split(File.separator);
        // String fileRegex = ".*[\\/]((.*_.*_.*_.*_.*_.*)_(.*)_(.*)_([01])(_(.*))?)(-processed)?\\.log";
        // Pattern pattern = Pattern.compile(fileRegex);
        // Matcher matcher = pattern.matcher("graphviz/2018_07_25_03_14_38_84151203_40b3c20f7e839725ae8d21531de347c2db2c1a2b_1_centos6build.log");
        // System.out.println(pattern.toString());
        // if(matcher.find()){
        //     System.out.println(matcher.group(1));;
        // }
        // String pathIn = "/sda/fregtrg/";
        //
        // if (!pathIn.endsWith(File.separator)){
        //     pathIn += File.separator;
        // }
        //
        // System.out.println(pathIn);
        ArrayList<String> hot = new ArrayList<>(Arrays.asList(new String[]{"remov", "distdir", "skip", "length"}));


        ArrayList<String> memoryWords = new ArrayList<>(hot);


        ArrayList<String> wordsNgram = memoryWords;
        memoryWords.set(0, "remove");
        System.out.println(memoryWords.get(0));
        System.out.println(wordsNgram.get(0));
        System.out.println(hot.get(0));


        // String[] regexes = new String[]{"https?://[^\\s]+", "hypothesisurlforge", "\n", " ", "[^\\s]+[/\\\\][^\\s]+", "hypothesispathforge", "[^\\s]+\\.[^\\s]+", "hypothesispathforge", "[\\d\\w]*\\w\\d[\\d\\w]*", "hypothesisnumletforge", "[\\d\\w]+\\d\\w[\\d\\w]*", "hypothesisnumletforge", "[_\\W]+", " ", "([A-Z]+)", " $1"};
        // List<MainExtract.ApplicableRegex> applicableRegexes = new ArrayList<>();
        // for(int i = 0; i < regexes.length; i+=2){
        //     applicableRegexes.add(new MainExtract.ApplicableRegex(Pattern.compile(regexes[i]), regexes[i+1]));
        // }
        // StringBuilder content = new StringBuilder();
        // BufferedReader reader = new BufferedReader(new FileReader("dataset/graphviz/2018_07_25_03_14_38_84151203_40b3c20f7e839725ae8d21531de347c2db2c1a2b_1_centos6build.log"));
        // String line;
        // while ((line = reader.readLine()) != null) {
        //     System.out.println(line);
        //     content.append(line).append("\n"); // 逐行添加到StringBuilder
        // }
        // // 关闭文件流
        // reader.close();
        // String con = content.toString();
        // for(MainExtract.ApplicableRegex applicableRegex : applicableRegexes){
        //     con = applicableRegex.regex.matcher(con).replaceAll(applicableRegex.replacement);
        //     System.out.println(applicableRegex.regex + applicableRegex.replacement+"\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        // }

        // int proc = 5;
        // // 创建一个 CountDownLatch，用于等待所有线程完成
        // CountDownLatch latch = new CountDownLatch(proc);
        // // 创建一个阻塞队列，用于传递文件名
        // BlockingQueue<String> files = new LinkedBlockingQueue<>();
        // files.put("A");
        // files.put("B");
        // files.put("C");
        // files.put("D");
        // files.put("E");
        // files.put("F");
        // files.put("G");
        //
        // ExecutorService executorService = Executors.newFixedThreadPool(proc);
        // // 启动多个线程处理文件
        // for (int i = 0; i < proc; i++) {
        //     executorService.submit(new ProcessFilesTask(latch, files));
        // }
        //
        //
        // try {
        //     // 等待所有线程完成
        //     latch.await();
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }
        // // 关闭线程池
        // executorService.shutdown();
        // System.out.println("END");
    }
    static class ProcessFilesTask implements Runnable {
        private static CountDownLatch latch;
        private static BlockingQueue<String> files;

        public ProcessFilesTask(CountDownLatch latch, BlockingQueue<String> files){
            ProcessFilesTask.latch = latch;
            ProcessFilesTask.files = files;
        }

        @Override
        public void run() {
            while(true){
                try {
                    String fileName = files.take(); // 如果队列为空，会阻塞等待文件名
                    System.out.println(fileName);
                    latch.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }
}
