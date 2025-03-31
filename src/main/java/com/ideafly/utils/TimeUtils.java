package com.ideafly.utils;

import java.time.Duration;
import java.time.LocalDateTime;

public class TimeUtils {

    /**
     * 根据给定的创建时间计算相对时间描述。
     * 例如：几小时前、几天前。
     *
     * @param createdAt 创建时间的 LocalDateTime 对象
     * @return 表示相对时间的字符串 (例如 "5小时前", "1天前", "3天前", "刚刚")
     */
    public static String formatRelativeTime(LocalDateTime createdAt) {
        if (createdAt == null) {
            return ""; // 或者根据需要返回其他默认值或抛出异常
        }

        LocalDateTime now = LocalDateTime.now();

        // 处理 createdAt 在未来的情况 (虽然不太常见，但最好处理)
        if (createdAt.isAfter(now)) {
            // 可以选择返回特定字符串，或者就当作“刚刚”处理
            return "刚刚";
        }

        Duration duration = Duration.between(createdAt, now);

        long days = duration.toDays(); // 获取相差的总天数 (按24小时计)
        long hours = duration.toHours(); // 获取相差的总小时数

        if (days > 0) {
            // 大于等于24小时，按天显示
            // duration.toDays() 会正确处理：
            // 24小时 -> 1天
            // 47小时 -> 1天
            // 48小时 -> 2天
            return String.format("%d天前", days);
        } else if (hours > 0) {
            // 小于24小时但大于等于1小时，按小时显示
            return String.format("%d小时前", hours);
        } else {
            // 小于1小时，可以显示“刚刚”或者分钟数 (这里按“刚刚”处理)
            // long minutes = duration.toMinutes();
            // return String.format("%d分钟前", minutes);
            return "刚刚";
        }
    }
}