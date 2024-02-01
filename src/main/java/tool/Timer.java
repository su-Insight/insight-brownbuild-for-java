package tool;

import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.time.DurationFormatUtils.formatDuration;

/**
 * @Author: Insight
 * @Description: TODO
 * @Date: 2024/1/6 下午10:27
 * @Version: 1.0
 */
public class Timer {
    public static String durationTimeFormat(long durationMills){
        long hours = TimeUnit.MILLISECONDS.toHours(durationMills);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMills) % 60;
        double seconds = TimeUnit.MILLISECONDS.toSeconds(durationMills) % 60 +
                (durationMills % 1000) / 1000.0; // 添加毫秒的小数部分

        // 构建格式化的时间字符串
        StringBuilder formattedTime = new StringBuilder();
        if (hours > 0) {
            formattedTime.append(hours).append("h ");
        }
        if (minutes > 0) {
            formattedTime.append(minutes).append("min ");
        }
        if (seconds < 10) {
            formattedTime.append(String.format("%.2f", seconds)).append(" sec");
        } else {
            formattedTime.append(String.format("%.2f", seconds)).append(" sec");
        }

        return formattedTime.toString();
    }
}
