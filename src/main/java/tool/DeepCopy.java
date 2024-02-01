package tool;

import java.io.*;
import java.util.Map;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/19 下午3:19
 * @Version: 1.0
 */
public class DeepCopy {
    public static <K, V> Map<K, V> deepCopyMap(Map<K, V> originalMap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(originalMap);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);

            return (Map<K, V>) ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
