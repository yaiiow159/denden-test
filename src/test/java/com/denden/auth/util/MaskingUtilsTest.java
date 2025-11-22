package com.denden.auth.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MaskingUtils 單元測試
 */
class MaskingUtilsTest {

    @ParameterizedTest
    @CsvSource({
        "user@example.com, u***@example.com",
        "john.doe@company.com, j***@company.com",
        "a@test.com, a***@test.com",
        "test@gmail.com, t***@gmail.com",
        "admin@localhost, a***@localhost"
    })
    void testMaskEmail_ValidEmails(String input, String expected) {
        assertThat(MaskingUtils.maskEmail(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testMaskEmail_NullOrEmpty(String input) {
        assertThat(MaskingUtils.maskEmail(input)).isEmpty();
    }

    @Test
    void testMaskEmail_NoAtSymbol() {
        assertThat(MaskingUtils.maskEmail("invalid")).isEqualTo("invalid");
    }

    @ParameterizedTest
    @CsvSource({
        "abc123def456ghi789, abc123de***",
        "verylongtoken123456789, verylong***"
    })
    void testMaskToken_ValidTokens(String input, String expected) {
        assertThat(MaskingUtils.maskToken(input)).isEqualTo(expected);
    }
    
    @Test
    void testMaskToken_ExactlyEightChars() {
        assertThat(MaskingUtils.maskToken("short123")).isEqualTo("***");
    }

    @Test
    void testMaskToken_ShortToken() {
        assertThat(MaskingUtils.maskToken("short")).isEqualTo("***");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testMaskToken_NullOrEmpty(String input) {
        assertThat(MaskingUtils.maskToken(input)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "0912345678, 0912***678",
        "886912345678, 8869***678",
        "0223456789, 0223***789"
    })
    void testMaskPhone_ValidPhones(String input, String expected) {
        assertThat(MaskingUtils.maskPhone(input)).isEqualTo(expected);
    }

    @Test
    void testMaskPhone_ShortPhone() {
        assertThat(MaskingUtils.maskPhone("12345")).isEqualTo("***");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testMaskPhone_NullOrEmpty(String input) {
        assertThat(MaskingUtils.maskPhone(input)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "A123456789, A***9",
        "B234567890, B***0",
        "C345678901, C***1"
    })
    void testMaskIdNumber_ValidIds(String input, String expected) {
        assertThat(MaskingUtils.maskIdNumber(input)).isEqualTo(expected);
    }

    @Test
    void testMaskIdNumber_ShortId() {
        assertThat(MaskingUtils.maskIdNumber("AB")).isEqualTo("***");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testMaskIdNumber_NullOrEmpty(String input) {
        assertThat(MaskingUtils.maskIdNumber(input)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "1234567890123456, ***3456",
        "4111111111111111, ***1111",
        "5500000000000004, ***0004"
    })
    void testMaskCardNumber_ValidCards(String input, String expected) {
        assertThat(MaskingUtils.maskCardNumber(input)).isEqualTo(expected);
    }

    @Test
    void testMaskCardNumber_ShortCard() {
        assertThat(MaskingUtils.maskCardNumber("1234")).isEqualTo("***");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testMaskCardNumber_NullOrEmpty(String input) {
        assertThat(MaskingUtils.maskCardNumber(input)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "1234567890, 2, 2, 12***90",
        "abcdefgh, 3, 2, abc***gh",
        "test, 1, 1, t***t"
    })
    void testMask_CustomPrefixSuffix(String input, int prefix, int suffix, String expected) {
        assertThat(MaskingUtils.mask(input, prefix, suffix)).isEqualTo(expected);
    }

    @Test
    void testMask_TooShort() {
        assertThat(MaskingUtils.mask("ab", 1, 1)).isEqualTo("***");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testMask_NullOrEmpty(String input) {
        assertThat(MaskingUtils.mask(input, 1, 1)).isEmpty();
    }
}
