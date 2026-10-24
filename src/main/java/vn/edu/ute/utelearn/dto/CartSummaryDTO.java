package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartSummaryDTO {

    @Builder.Default
    private List<CartItemDTO> items = new ArrayList<>();

    @Builder.Default
    private BigDecimal originalTotal = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal finalTotal = BigDecimal.ZERO;

    private String discountCode;
    private String discountMessage;
    private boolean discountValid;

    public String getFormattedOriginalTotal() {
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(originalTotal) + " VNĐ";
    }

    public String getFormattedDiscountAmount() {
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(discountAmount) + " VNĐ";
    }

    public String getFormattedFinalTotal() {
        if (finalTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return "0 VNĐ (Miễn phí)";
        }
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(finalTotal) + " VNĐ";
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDTO {
        private Long id;
        private String itemType;
        private Long itemId;
        private String title;
        private String codeOrSlug;
        private BigDecimal price;
        private String formattedPrice;
        private String imageUrl;
        private boolean selected;
    }
}
