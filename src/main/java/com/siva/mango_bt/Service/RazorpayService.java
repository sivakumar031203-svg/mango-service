package com.siva.mango_bt.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    /**
     * Creates a Razorpay order.
     * amount is in INR (e.g. 499.00) — we convert to paise internally.
     */
    public JSONObject createOrder(String receiptId, BigDecimal amount) throws RazorpayException {
        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject options = new JSONObject();
        options.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue()); // paise
        options.put("currency", "INR");
        options.put("receipt", receiptId);           // your order number e.g. MNG-17123456
        options.put("payment_capture", 1);           // auto-capture

        Order razorpayOrder = client.orders.create(options);
        return razorpayOrder.toJson();
    }

    /**
     * Verifies the payment signature sent by Razorpay after successful payment.
     * Must be called before marking an order as PAID.
     *
     * Formula: HMAC-SHA256( razorpayOrderId + "|" + razorpayPaymentId, keySecret )
     */
    public boolean verifySignature(String razorpayOrderId,
                                   String razorpayPaymentId,
                                   String razorpaySignature) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generated = HexFormat.of().formatHex(hash);
            return generated.equals(razorpaySignature);
        } catch (Exception e) {
            return false;
        }
    }

    public String getKeyId() {
        return keyId;
    }
}
