package vn.edu.ute.utelearn.util;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class ValidationUtils {

    // Regex chuẩn để kiểm tra Email
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    // Regex kiểm tra mật khẩu mạnh (Ít nhất 8 ký tự, 1 chữ hoa, 1 chữ thường, 1 số, 1 ký tự đặc biệt)
    private static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_]).{8,}$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    // Regex kiểm tra số điện thoại Việt Nam (Đầu 0 hoặc +84)
    private static final String PHONE_REGEX = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$";
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);

    /**
     * Kiểm tra chuỗi rỗng hoặc null (chỉ toàn khoảng trắng cũng tính là rỗng)
     */
    public static boolean isNullOrEmpty(String str) {
        return !StringUtils.hasText(str);
    }

    /**
     * Kiểm tra độ dài tối thiểu và tối đa của chuỗi
     */
    public static boolean isValidLength(String str, int min, int max) {
        if (isNullOrEmpty(str)) return false;
        int length = str.trim().length();
        return length >= min && length <= max;
    }

    /**
     * Kiểm tra định dạng Email hợp lệ
     */
    public static boolean isValidEmail(String email) {
        if (isNullOrEmpty(email)) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Kiểm tra định dạng Mật khẩu mạnh
     */
    public static boolean isStrongPassword(String password) {
        if (isNullOrEmpty(password)) return false;
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Kiểm tra số điện thoại hợp lệ
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (isNullOrEmpty(phone)) return false;
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Loại bỏ các thẻ HTML để chống XSS (Sanitize HTML)
     */
    public static String stripHtmlTags(String input) {
        if (isNullOrEmpty(input)) return input;
        return input.replaceAll("<[^>]*>", "").trim();
    }

    /**
     * Chuyển đổi ký tự đặc biệt thành mã HTML để hiển thị an toàn (Encode HTML)
     * Thường dùng khi muốn giữ lại ký tự thay vì xóa đi.
     */
    public static String encodeHtml(String input) {
        if (isNullOrEmpty(input)) return input;
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
