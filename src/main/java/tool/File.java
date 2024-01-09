package tool;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/6 下午10:28
 * @Version: 1.0
 */
public class File {
    public static void checkAndMkdir(String filePath){
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)){
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public static List<String> getPathFiles(String pathIn){
        return Arrays.asList(Objects.requireNonNull(new java.io.File(pathIn).list()));
    }
}
