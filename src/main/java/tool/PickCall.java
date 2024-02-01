package tool;

import base.DataFrame;
import base.Experiment;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static tool.File.checkAndMkdir;
import static tool.File.checkAndTouch;
import static tool.Timer.durationTimeFormat;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/4 下午7:11
 * @Version: 1.0
 */
public class PickCall {
    public static <T> void serialize(T data, String fileName){
        try {
            checkAndTouch(fileName);
            FileOutputStream fileOut = new FileOutputStream(fileName);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserialize(String fileName){
        T data = null;
        try {
            FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileIn);
            data = (T) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    /**
     * @Author Insight 
     * @Date 2024/1/6 下午5:53
     * @Description This is description of method
     * @Param [cla, method, args, fileName, recompute]
     * "cla" represents the instance object of the class where the method is located
     * "method" represents the object of the method to be executed.
     * @Return void
     * @Since version 1.0
     */
    public static <T> T runAndSerialize(Class<?> cla, Method method, Object[] args, String fileName, boolean recompute) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        long start = System.currentTimeMillis();
        T res;
        System.out.print("Load " + fileName + " ... ");
        if (!recompute && Files.exists(Paths.get(fileName))){
            res = deserialize(fileName);
        }else {
            Object instance = null;  // 默认情况下初始化为 null
            // 如果方法不是静态的，创建类的实例
            if (!Modifier.isStatic(method.getModifiers())) {
                instance = cla.getDeclaredConstructor().newInstance();
            }

            System.out.print("(computing) ...");
            res = (T) method.invoke(instance, args);
            serialize(res, fileName);
        }
        System.out.println("Done in " + durationTimeFormat(System.currentTimeMillis() - start));
        return res;
    }

    public static void main(String[] args) throws NoSuchMethodException {
        // runAndSerialize(GetData.class.getMethod(""));
    }

}
