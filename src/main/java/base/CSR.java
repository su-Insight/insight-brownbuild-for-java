package base;

import org.apache.commons.math3.linear.OpenMapRealMatrix;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/27 上午9:47
 * @Version: 1.0
 */
public class CSR implements Serializable {
    public long[] rowHeaders;
    public float[] data;
    public int[] colIndex;
    public int numColumn;
    public int nonZeroCount = 0;
    public double[][] originalData;

    public CSR(double[][] matrix, int colNum, int rowNum) {
        originalData = matrix;
        numColumn = colNum;
        int index = 0;
        int _headIndex = 0;
        int headIndex = 0;
        int columnIndex = 0;

        nonZeroCount = Arrays.stream(matrix)
                .parallel()
                .flatMapToDouble(Arrays::stream)
                .filter(value -> value != 0.0)
                .mapToInt(e -> 1)
                .sum();

        data = new float[nonZeroCount];
        rowHeaders = new long[rowNum + 1];
        colIndex = new int[nonZeroCount];

        for (int i = 0; i < rowNum; i++) {
            boolean isHead = false;
            for (int j = 0; j < colNum; j++) {
                if (matrix[i][j] != 0.0) {
                    if (!isHead) {
                        isHead = true;
                        rowHeaders[_headIndex++] = headIndex++;
                    }else {
                        headIndex++;
                    }
                    data[index++] = (float)matrix[i][j];
                    colIndex[columnIndex++] = j;
                }
            }
        }
        rowHeaders[_headIndex] = nonZeroCount;
    }

    private int findRowIndex(int itemIndex) {
        int index = Arrays.binarySearch(rowHeaders, itemIndex);

        if (index < 0) {
            // 如果找不到精确匹配，将返回一个负值，表示不在数组中
            // 计算小于等于目标值的最大元素的索引
            index = -(index + 2);
        }

        // 如果 index 为负数，表示目标值小于数组中所有元素
        // 如果 index 为非负数，表示目标值小于或等于数组中的某个元素
        return index;
    }

    public double getElement(int i, int j) {
        long rowStart = rowHeaders[i];
        long rowEnd = rowHeaders[i + 1];

        for (long k = rowStart; k < rowEnd; k++) {
            if (colIndex[(int)k] == j) {
                return data[(int)k];
            }
        }

        // 如果没有找到元素，则返回默认值（例如0）
        return 0.0;
    }

    public void print(){
        int numRows = rowHeaders.length - 1;

        for (int row = 0; row < numRows; row++) {
            long start = rowHeaders[row];
            long end = rowHeaders[row + 1];

            for (long i = start; i < end; i++) {
                int col = colIndex[(int) i];
                double value = data[(int) i];
                System.out.println("(" + row + ", " + col + ") " + value);
            }
        }
    }

    public void print(int MAX_PRINT_LENGTH){
        int n = MAX_PRINT_LENGTH / 2;
        // 输出前半部分
        int headerIndex = 0;

        int col;
        int row;
        for (int i = 0; i < MAX_PRINT_LENGTH / 2; i++) {
            col = colIndex[i];
            if (i >= rowHeaders[headerIndex] && i < rowHeaders[headerIndex+1]) {
                row = headerIndex;
            }else {
                row = headerIndex++;
            }
            System.out.println("(" + row + ", " + col + ") " + data[i]);
        }


        System.out.println("...");

        // 输出后半部分
        headerIndex = findRowIndex(nonZeroCount - MAX_PRINT_LENGTH / 2);
        for (int i = nonZeroCount - MAX_PRINT_LENGTH / 2; i < nonZeroCount / 2; i++) {
            col = colIndex[i];
            if (i >= rowHeaders[headerIndex] && i < rowHeaders[headerIndex+1]) {
                row = headerIndex;
            }else {
                row = headerIndex++;
            }
            System.out.println("(" + row + ", " + col + ") " + data[i]);
        }
    }
}
