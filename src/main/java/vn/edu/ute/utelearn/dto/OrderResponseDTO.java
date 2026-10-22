package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private String orderCode;
    private String username;
    private String userFullName;
    private String userEmail;
    private BigDecimal totalAmount;
    private String formattedTotalAmount;
    private String status;
    private String statusDisplayName;
    private String statusBadgeClass;
    private String paymentMethod;
    private String transactionId;
    private String createdAt;
    private String paidAt;
    private List<OrderItemDTO> items;
    private String primaryItemName;
    private String targetLearnUrl;
}
