package com.boonya.business.trip.common.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 时间计算工具类
 */
public class TimeCalcUtil {

    /**
     * 计算两个时间之间的天数、小时、分钟
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间差信息，格式：X 天 Y 小时 Z 分钟
     */
    public static String calculateTimeDifference(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "";
        }

        Duration duration = Duration.between(startTime, endTime);
        long totalSeconds = duration.getSeconds();

        if (totalSeconds < 0) {
            totalSeconds = -totalSeconds;
        }

        long days = totalSeconds / (24 * 3600);
        long remainingSeconds = totalSeconds % (24 * 3600);
        long hours = remainingSeconds / 3600;
        long minutes = (remainingSeconds % 3600) / 60;

        StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days).append("天");
        }
        if (hours > 0 || days > 0) {
            result.append(hours).append("小时");
        }
        result.append(minutes).append("分钟");

        return result.toString();
    }

    /**
     * 计算两个时间之间的天数
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 天数
     */
    public static long calculateDays(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startTime, endTime);
    }

    /**
     * 计算两个时间之间的小时数
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 小时数
     */
    public static long calculateHours(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(startTime, endTime);
    }

    /**
     * 计算两个时间之间的分钟数
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分钟数
     */
    public static long calculateMinutes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }

    /**
     * 计算两个时间之间的总小时数（包含天数转换）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 总小时数
     */
    public static long calculateTotalHours(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }
        Duration duration = Duration.between(startTime, endTime);
        return Math.abs(duration.toHours());
    }

    /**
     * 格式化飞行时间/行程时间
     *
     * @param departureTime 出发时间
     * @param arrivalTime 到达时间
     * @return 格式化的时间描述，如："2 小时 30 分钟"
     */
    public static String formatTravelTime(LocalDateTime departureTime, LocalDateTime arrivalTime) {
        if (departureTime == null || arrivalTime == null) {
            return "时间待定";
        }

        Duration duration = Duration.between(departureTime, arrivalTime);
        long totalMinutes = Math.abs(duration.toMinutes());

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours > 0 && minutes > 0) {
            return hours + "小时" + minutes + "分钟";
        } else if (hours > 0) {
            return hours + "小时";
        } else {
            return minutes + "分钟";
        }
    }
}
