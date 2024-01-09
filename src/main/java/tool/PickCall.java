package tool;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static tool.Timer.durationTimeFormat;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/4 下午7:11
 * @Version: 1.0
 */
public class PickCall {
    public static void serialize(Object data, String fileName){
        try {
            FileOutputStream fileOut = new FileOutputStream(fileName);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object deserialize(String fileName){
        Object obj = null;
        try {
            FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileIn);
            obj = objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return obj;
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
    public static Object runAndSerialize(Class cla, Method method, Map<String, Object> args, String fileName, boolean recompute) throws InvocationTargetException, IllegalAccessException {
        long start = System.currentTimeMillis();
        Object obj = new Object();
        System.out.print("Load " + fileName + " ... ");
        if (!recompute && Files.exists(Paths.get(fileName))){
            obj = deserialize(fileName);
        }else {
            System.out.print("(computing) ...");
            Object object = method.invoke(cla, args);
            serialize(object, fileName);
        }
        System.out.print("Done in " + durationTimeFormat(System.currentTimeMillis() - start) + " sec");
        return obj;
    }

    public static void main(String[] args) throws NoSuchMethodException {
        // runAndSerialize(GetData.class.getMethod(""));
    }

}
