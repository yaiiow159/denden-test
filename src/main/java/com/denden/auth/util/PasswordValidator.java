package com.denden.auth.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 密碼驗證工具
 */
public final class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 100;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?]");

    private PasswordValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static boolean isValid(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        return password.length() >= MIN_LENGTH
                && password.length() <= MAX_LENGTH
                && UPPERCASE_PATTERN.matcher(password).find()
                && LOWERCASE_PATTERN.matcher(password).find()
                && DIGIT_PATTERN.matcher(password).find()
                && SPECIAL_CHAR_PATTERN.matcher(password).find();
    }

    public static ValidationResult validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("密碼不可為空");
            return new ValidationResult(false, errors);
        }

        if (password.length() < MIN_LENGTH) {
            errors.add(String.format("密碼長度至少需要 %d 個字元", MIN_LENGTH));
        }

        if (password.length() > MAX_LENGTH) {
            errors.add(String.format("密碼長度不可超過 %d 個字元", MAX_LENGTH));
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("密碼必須包含至少一個大寫字母 (A-Z)");
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            errors.add("密碼必須包含至少一個小寫字母 (a-z)");
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            errors.add("密碼必須包含至少一個數字 (0-9)");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            errors.add("密碼必須包含至少一個特殊字元 (!@#$%^&*()_+-=[]{}|;:,.<>?)");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public String getFirstError() {
            return errors.isEmpty() ? null : errors.get(0);
        }

        public String getAllErrorsAsString() {
            return String.join("\n", errors);
        }

        @Override
        public String toString() {
            return "ValidationResult{" +
                    "valid=" + valid +
                    ", errors=" + errors +
                    '}';
        }
    }
}
