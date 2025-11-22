package com.denden.auth.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TimeUtils 測試
 */
class TimeUtilsTest {

    @Test
    void toTimestamp_應該正確轉換LocalDateTime為毫秒時間戳() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        
        // When
        long timestamp = TimeUtils.toTimestamp(dateTime);
        
        // Then
        assertThat(timestamp).isPositive();
        
        // 驗證可以轉換回來
        LocalDateTime converted = TimeUtils.fromTimestamp(timestamp);
        assertThat(converted).isEqualTo(dateTime);
    }

    @Test
    void toTimestamp_當傳入null時應該拋出異常() {
        // When & Then
        assertThatThrownBy(() -> TimeUtils.toTimestamp(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("dateTime cannot be null");
    }

    @Test
    void toTimestampAsDouble_應該返回double類型的時間戳() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        
        // When
        double timestamp = TimeUtils.toTimestampAsDouble(dateTime);
        
        // Then
        assertThat(timestamp).isPositive();
        
        // 驗證與 long 版本一致
        long timestampLong = TimeUtils.toTimestamp(dateTime);
        assertThat(timestamp).isEqualTo((double) timestampLong);
    }

    @Test
    void fromTimestamp_應該正確轉換毫秒時間戳為LocalDateTime() {
        // Given
        long timestamp = 1704096000000L; // 2024-01-01 12:00:00 (UTC+8)
        
        // When
        LocalDateTime dateTime = TimeUtils.fromTimestamp(timestamp);
        
        // Then
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(2024);
        assertThat(dateTime.getMonthValue()).isEqualTo(1);
        assertThat(dateTime.getDayOfMonth()).isEqualTo(1);
    }

    @Test
    void fromTimestamp_double版本應該正確轉換() {
        // Given
        double timestamp = 1704096000000.0;
        
        // When
        LocalDateTime dateTime = TimeUtils.fromTimestamp(timestamp);
        
        // Then
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(2024);
    }

    @Test
    void toTimestampSeconds_應該正確轉換為秒級時間戳() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        
        // When
        long timestampSeconds = TimeUtils.toTimestampSeconds(dateTime);
        
        // Then
        assertThat(timestampSeconds).isPositive();
        
        // 驗證秒級時間戳是毫秒級的 1/1000
        long timestampMillis = TimeUtils.toTimestamp(dateTime);
        assertThat(timestampSeconds).isEqualTo(timestampMillis / 1000);
    }

    @Test
    void fromTimestampSeconds_應該正確轉換秒級時間戳() {
        // Given
        long timestampSeconds = 1704096000L; // 2024-01-01 12:00:00 (UTC+8)
        
        // When
        LocalDateTime dateTime = TimeUtils.fromTimestampSeconds(timestampSeconds);
        
        // Then
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(2024);
        assertThat(dateTime.getMonthValue()).isEqualTo(1);
        assertThat(dateTime.getDayOfMonth()).isEqualTo(1);
    }

    @Test
    void 往返轉換應該保持一致性() {
        // Given
        LocalDateTime original = LocalDateTime.of(2024, 6, 15, 14, 30, 45);
        
        // When - 毫秒級往返
        long timestamp = TimeUtils.toTimestamp(original);
        LocalDateTime converted = TimeUtils.fromTimestamp(timestamp);
        
        // Then
        assertThat(converted).isEqualTo(original);
        
        // When - 秒級往返（會失去毫秒精度）
        long timestampSeconds = TimeUtils.toTimestampSeconds(original);
        LocalDateTime convertedSeconds = TimeUtils.fromTimestampSeconds(timestampSeconds);
        
        // Then - 秒級精度
        assertThat(convertedSeconds.getYear()).isEqualTo(original.getYear());
        assertThat(convertedSeconds.getMonthValue()).isEqualTo(original.getMonthValue());
        assertThat(convertedSeconds.getDayOfMonth()).isEqualTo(original.getDayOfMonth());
        assertThat(convertedSeconds.getHour()).isEqualTo(original.getHour());
        assertThat(convertedSeconds.getMinute()).isEqualTo(original.getMinute());
        assertThat(convertedSeconds.getSecond()).isEqualTo(original.getSecond());
    }

    @Test
    void double和long版本應該產生相同結果() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 20, 10, 15, 30);
        
        // When
        long timestampLong = TimeUtils.toTimestamp(dateTime);
        double timestampDouble = TimeUtils.toTimestampAsDouble(dateTime);
        
        LocalDateTime fromLong = TimeUtils.fromTimestamp(timestampLong);
        LocalDateTime fromDouble = TimeUtils.fromTimestamp(timestampDouble);
        
        // Then
        assertThat(fromLong).isEqualTo(fromDouble);
        assertThat(fromLong).isEqualTo(dateTime);
    }
}
