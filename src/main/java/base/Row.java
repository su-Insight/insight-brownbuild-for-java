package base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/10 下午7:13
 * @Version: 1.0
 */
public class Row extends ArrayList<Object>  implements Serializable {
    public Row(List<Object> data) {
        super(data);
    }
    public Row() {}
}
