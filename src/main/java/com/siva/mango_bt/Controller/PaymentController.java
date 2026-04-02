package com.siva.mango_bt.Controller;


import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.siva.mango_bt.Entity.Order;
import com.siva.mango_bt.Repos.OrderRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Autowired
    private OrderRepository orderRepository;

    // ── Step 1: Create a Razorpay order ──────────────────────────────────────
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, String> body) {
        String orderNumber = body.get("orderNumber");

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElse(null);
        if (order == null)
            return ResponseEntity.badRequest().body(Map.of("message", "Order not found"));

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject options = new JSONObject();
            // Razorpay expects amount in paise (multiply ₹ by 100)
            options.put("amount", order.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100)).intValue());
            options.put("currency", "INR");
            options.put("receipt", orderNumber);

            com.razorpay.Order rzpOrder = client.orders.create(options);

            return ResponseEntity.ok(Map.of(
                    "razorpayOrderId", rzpOrder.get("id"),
                    "amount",          rzpOrder.get("amount"),
                    "currency",        rzpOrder.get("currency"),
                    "keyId",           keyId,
                    "customerName",    order.getCustomerName(),
                    "customerEmail",   order.getCustomerEmail() != null ? order.getCustomerEmail() : "",
                    "customerPhone",   order.getCustomerPhone() != null ? order.getCustomerPhone() : ""
            ));

        } catch (RazorpayException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Razorpay error: " + e.getMessage()));
        }
    }

    // ── Step 2: Verify signature after payment ────────────────────────────────
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> body) {
        String razorpayOrderId   = body.get("razorpayOrderId");
        String razorpayPaymentId = body.get("razorpayPaymentId");
        String razorpaySignature = body.get("razorpaySignature");
        String orderNumber       = body.get("orderNumber");

        // Verify HMAC-SHA256 signature
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        if (!isValidSignature(payload, razorpaySignature, keySecret)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid payment signature"));
        }

        // Mark the order as paid
        return orderRepository.findByOrderNumber(orderNumber).map(order -> {
            order.setUpiTransactionId(razorpayPaymentId);
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);
            return ResponseEntity.ok(Map.of(
                    "message",     "Payment verified successfully!",
                    "orderNumber", orderNumber,
                    "paymentId",   razorpayPaymentId
            ));
        }).orElse(ResponseEntity.badRequest()
                .body(Map.of("message", "Order not found")));
    }

    // ── HMAC-SHA256 helper ────────────────────────────────────────────────────
    private boolean isValidSignature(String data, String signature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes());
            String generated = HexFormat.of().formatHex(hash);
            return generated.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}