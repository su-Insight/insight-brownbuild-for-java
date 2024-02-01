package tool;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
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
    public static void checkAndTouch(String filePath){
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)){
                Files.createFile(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static StringBuffer getFileContent(String filePath){
        StringBuffer content = new StringBuffer();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Read file error. ：" + filePath, e);
        }
        return content;
    }

    public static List<String> getPathFiles(String pathIn){
        try {
            return Arrays.asList(Objects.requireNonNull(new java.io.File(pathIn).list()));
        } catch (NullPointerException e) {
            throw new InvalidPathException(pathIn, "PathData is error, the file list is empty.");
        }

    }
}
