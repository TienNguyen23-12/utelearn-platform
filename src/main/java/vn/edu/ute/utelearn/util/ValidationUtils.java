package vn.edu.ute.utelearn.util;
import org.springframework.util.StringUtils;
import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;
public class ValidationUtils {
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    public static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9\\s]).{8,}$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);
    private static final String PHONE_REGEX = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$";
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);
    private static final Pattern NON_VIDEO_EXT_PATTERN = Pattern.compile(".*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|7z|png|jpg|jpeg|gif|svg|webp|bmp|ico|txt|csv|mp3|wav|html|htm)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern YOUTUBE_VIDEO_PATTERN = Pattern.compile("(?:youtu\\.be/|youtube(?:-nocookie)?\\.com/(?:embed/|v/|watch\\?v=|shorts/|live/|watch\\?.+&v=))([\\w-]{11})", Pattern.CASE_INSENSITIVE);
    private static final Pattern VIMEO_VIDEO_PATTERN = Pattern.compile("vimeo\\.com/(?:channels/(?:\\w+/)|groups/[^/]*/videos/|album/\\d+/video/|video/|)(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIRECT_VIDEO_EXT_PATTERN = Pattern.compile(".*\\.(mp4|webm|ogg|ogv|mov|m4v|mkv|avi|m3u8)$", Pattern.CASE_INSENSITIVE);
    public static boolean isNullOrEmpty(String str) {
        return !StringUtils.hasText(str);
    }
    public static boolean isValidLength(String str, int min, int max) {
        if (isNullOrEmpty(str)) return false;
        int length = str.trim().length();
        return length >= min && length <= max;
    }
    public static boolean isValidEmail(String email) {
        if (isNullOrEmpty(email)) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }
    public static boolean isStrongPassword(String password) {
        if (isNullOrEmpty(password)) return false;
        return PASSWORD_PATTERN.matcher(password).matches();
    }
    public static boolean isValidPhoneNumber(String phone) {
        if (isNullOrEmpty(phone)) return false;
        return PHONE_PATTERN.matcher(phone).matches();
    }
    public static boolean isValidVideoUrl(String url) {
        if (isNullOrEmpty(url)) {
            return false;
        }
        String trimmed = url.trim();
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return false;
            }
            String lowerScheme = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(lowerScheme) && !"https".equals(lowerScheme)) {
                return false;
            }
            String lowerHost = host.toLowerCase(Locale.ROOT);
            String path = uri.getPath() != null ? uri.getPath().toLowerCase(Locale.ROOT) : "";
            String lowerFull = trimmed.toLowerCase(Locale.ROOT);

            if (lowerFull.contains("/image/upload/") || lowerFull.contains("/raw/upload/") || lowerHost.contains("docs.google.com")) {
                return false;
            }
            if (NON_VIDEO_EXT_PATTERN.matcher(path).matches()) {
                return false;
            }
            if (lowerHost.contains("youtube.com") || lowerHost.contains("youtu.be") || lowerHost.contains("youtube-nocookie.com")) {
                return YOUTUBE_VIDEO_PATTERN.matcher(trimmed).find();
            }
            if (lowerHost.contains("vimeo.com")) {
                return VIMEO_VIDEO_PATTERN.matcher(trimmed).find();
            }
            if (lowerHost.contains("drive.google.com")) {
                return path.contains("/file/d/") || (uri.getQuery() != null && uri.getQuery().contains("id="));
            }
            return lowerFull.contains("/video/upload/") || lowerFull.contains("/video/") || DIRECT_VIDEO_EXT_PATTERN.matcher(path).matches();
        } catch (Exception e) {
            return false;
        }
    }
    public static String stripHtmlTags(String input) {
        if (isNullOrEmpty(input)) return input;
        return input.replaceAll("<[^>]*>", "").trim();
    }
    public static String encodeHtml(String input) {
        if (isNullOrEmpty(input)) return input;
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
