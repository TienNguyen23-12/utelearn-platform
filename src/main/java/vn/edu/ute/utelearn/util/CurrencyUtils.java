package vn.edu.ute.utelearn.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyUtils {

    private CurrencyUtils() {
    }

    public static String formatVnd(BigDecimal amount) {
        if (amount == null) {
            return "0 VNĐ";
        }
        return NumberFormat.getNumberInstance(Locale.US).format(amount) + " VNĐ";
    }

    public static String formatVnd(Number amount) {
        if (amount == null) {
            return "0 VNĐ";
        }
        return NumberFormat.getNumberInstance(Locale.US).format(amount) + " VNĐ";
    }
}
