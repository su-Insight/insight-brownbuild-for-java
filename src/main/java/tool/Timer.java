package tool;

import static org.apache.commons.lang.time.DurationFormatUtils.formatDuration;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/6 下午10:27
 * @Version: 1.0
 */
public class Timer {
    public static String durationTimeFormat(long durationMills){
        return formatDuration(durationMills, "H'h'mm'm'ss.SSS's'");
    }
}
