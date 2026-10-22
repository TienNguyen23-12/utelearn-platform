package vn.edu.ute.utelearn.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ute.utelearn.dto.OrderResponseDTO;
import vn.edu.ute.utelearn.dto.SepayWebhookDTO;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.service.OrderService;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class SepayWebhookApiController {

    private final OrderService orderService;

    @PostMapping("/api/webhooks/sepay")
    public ResponseEntity<Map<String, Object>> handleSepayWebhook(
            @RequestBody(required = false) SepayWebhookDTO payload,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        log.info("[SepayWebhookApiController] Nhận webhook từ SePay: {}", payload);

        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Payload rỗng"));
        }

        boolean success = orderService.processSepayWebhook(payload, authorizationHeader);
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xử lý webhook đối soát thành công"
            ));
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "success", false,
                "message", "Giao dịch không khớp đơn hàng hoặc API key không hợp lệ"
        ));
    }

    @GetMapping("/api/orders/{orderCode}/status")
    public ResponseEntity<Map<String, Object>> checkOrderStatus(@PathVariable String orderCode) {
        Optional<Order> orderOpt = orderService.findByOrderCode(orderCode);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy đơn hàng"
            ));
        }

        Order order = orderOpt.get();
        OrderResponseDTO dto = orderService.toResponseDTO(order);
        boolean isPaid = "SUCCESS".equalsIgnoreCase(order.getStatus()) || "COMPLETED".equalsIgnoreCase(order.getStatus());

        return ResponseEntity.ok(Map.of(
                "orderCode", order.getOrderCode(),
                "status", order.getStatus(),
                "isPaid", isPaid,
                "statusDisplayName", order.getStatusDisplayName(),
                "redirectUrl", dto.getTargetLearnUrl()
        ));
    }
}
