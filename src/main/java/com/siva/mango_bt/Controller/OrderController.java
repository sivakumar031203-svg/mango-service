package com.siva.mango_bt.Controller;

import com.siva.mango_bt.DTO.OrderRequest;
import com.siva.mango_bt.Entity.Mango;
import com.siva.mango_bt.Entity.Order;
import com.siva.mango_bt.Entity.OrderItem;
import com.siva.mango_bt.Repos.MangoRepository;
import com.siva.mango_bt.Repos.OrderRepository;
import com.siva.mango_bt.Service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MangoRepository mangoRepository;

    @Autowired
    private NotificationService notificationService;

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        try {
            if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mobile number is required"));
            }
            if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Delivery address is required"));
            }

            List<OrderItem> items = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;

            for (OrderRequest.CartItem cartItem : request.getItems()) {
                Mango mango = mangoRepository.findById(cartItem.getMangoId())
                        .orElseThrow(() -> new RuntimeException("Mango not found: " + cartItem.getMangoId()));
                if (!mango.getIsAvailable()) {
                    return ResponseEntity.badRequest().body(Map.of("message", mango.getName() + " is not available"));
                }
                if (mango.getStock() < cartItem.getQuantity()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Only " + mango.getStock() + " units left for " + mango.getName()));
                }
                BigDecimal itemTotal = mango.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                total = total.add(itemTotal);
                items.add(OrderItem.builder().mango(mango).quantity(cartItem.getQuantity()).unitPrice(mango.getPrice()).totalPrice(itemTotal).build());
            }

            String orderNumber = "MNG-" + System.currentTimeMillis();
            Order order = Order.builder()
                    .orderNumber(orderNumber)
                    .customerName(request.getCustomerName())
                    .customerEmail(request.getCustomerEmail())
                    .customerPhone(request.getCustomerPhone())
                    .deliveryAddress(request.getDeliveryAddress())
                    .landmark(request.getLandmark())
                    .pincode(request.getPincode())
                    .city(request.getCity())
                    .orderNotes(request.getOrderNotes())
                    .totalAmount(total)
                    .paymentMethod(Order.PaymentMethod.valueOf(request.getPaymentMethod()))
                    .status(Order.OrderStatus.PENDING)
                    .paymentStatus(Order.PaymentStatus.PENDING)
                    .isNewNotification(true)
                    .build();

            Order savedOrder = orderRepository.save(order);
            items.forEach(item -> item.setOrder(savedOrder));
            savedOrder.setItems(items);
            Order finalOrder = orderRepository.save(savedOrder);

            for (int i = 0; i < items.size(); i++) {
                Mango mango = items.get(i).getMango();
                mango.setStock(mango.getStock() - request.getItems().get(i).getQuantity());
                mangoRepository.save(mango);
            }

            notificationService.sendNewOrderNotification(
                    finalOrder.getOrderNumber(), finalOrder.getCustomerName(),
                    finalOrder.getCustomerPhone(), finalOrder.getTotalAmount().toString());

            return ResponseEntity.ok(Map.of(
                    "orderNumber", finalOrder.getOrderNumber(),
                    "orderId", finalOrder.getId(),
                    "totalAmount", finalOrder.getTotalAmount(),
                    "paymentMethod", finalOrder.getPaymentMethod(),
                    "message", "Order placed successfully!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @GetMapping("/track/{orderNumber}")
    public ResponseEntity<?> trackOrder(@PathVariable String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{orderNumber}/payment")
    public ResponseEntity<?> updatePayment(@PathVariable String orderNumber, @RequestBody Map<String, String> body) {
        return orderRepository.findByOrderNumber(orderNumber).map(order -> {
            order.setUpiTransactionId(body.get("transactionId"));
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);
            return ResponseEntity.ok(Map.of("message", "Payment confirmed!", "orderNumber", orderNumber));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search) {

        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        if (status != null && !status.isBlank()) {
            try {
                Order.OrderStatus s = Order.OrderStatus.valueOf(status.toUpperCase());
                orders = orders.stream().filter(o -> o.getStatus() == s).collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            orders = orders.stream().filter(o ->
                    o.getOrderNumber().toLowerCase().contains(q) ||
                            o.getCustomerName().toLowerCase().contains(q) ||
                            (o.getCustomerPhone() != null && o.getCustomerPhone().contains(q)) ||
                            (o.getCustomerEmail() != null && o.getCustomerEmail().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }

        Comparator<Order> comparator = switch (sortBy) {
            case "totalAmount" -> Comparator.comparing(Order::getTotalAmount);
            case "customerName" -> Comparator.comparing(Order::getCustomerName, String.CASE_INSENSITIVE_ORDER);
            case "status" -> Comparator.comparing(o -> o.getStatus().name());
            default -> Comparator.comparing(Order::getCreatedAt);
        };
        if ("desc".equalsIgnoreCase(sortDir)) comparator = comparator.reversed();
        orders.sort(comparator);

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return orderRepository.findById(id).map(order -> {
            String newStatus = body.get("status");
            order.setStatus(Order.OrderStatus.valueOf(newStatus));
            if ("DELIVERED".equals(newStatus)) {
                order.setDeliveredAt(LocalDateTime.now());
                order.setPaymentStatus(Order.PaymentStatus.PAID);
                order.setIsNewNotification(false);
            }
            if (body.containsKey("paymentStatus")) {
                order.setPaymentStatus(Order.PaymentStatus.valueOf(body.get("paymentStatus")));
            }
            return ResponseEntity.ok(orderRepository.save(order));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/deliver")
    public ResponseEntity<?> markDelivered(@PathVariable Long id) {
        return orderRepository.findById(id).map(order -> {
            order.setStatus(Order.OrderStatus.DELIVERED);
            order.setDeliveredAt(LocalDateTime.now());
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setIsNewNotification(false);
            Order saved = orderRepository.save(order);
            return ResponseEntity.ok(Map.of(
                    "message", "Order marked as delivered!",
                    "orderNumber", saved.getOrderNumber(),
                    "deliveredAt", saved.getDeliveredAt().toString()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats/summary")
    public ResponseEntity<?> getStats() {
        List<Order> all = orderRepository.findAll();
        BigDecimal revenue = all.stream().filter(o -> o.getPaymentStatus() == Order.PaymentStatus.PAID)
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayRevenue = all.stream()
                .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.PAID && o.getCreatedAt() != null &&
                        o.getCreatedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
                "totalOrders", all.size(),
                "pendingOrders", all.stream().filter(o -> o.getStatus() == Order.OrderStatus.PENDING).count(),
                "confirmedOrders", all.stream().filter(o -> o.getStatus() == Order.OrderStatus.CONFIRMED).count(),
                "processingOrders", all.stream().filter(o -> o.getStatus() == Order.OrderStatus.PROCESSING).count(),
                "shippedOrders", all.stream().filter(o -> o.getStatus() == Order.OrderStatus.SHIPPED).count(),
                "deliveredOrders", all.stream().filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED).count(),
                "cancelledOrders", all.stream().filter(o -> o.getStatus() == Order.OrderStatus.CANCELLED).count(),
                "totalRevenue", revenue,
                "todayRevenue", todayRevenue,
                "newNotifications", orderRepository.countByIsNewNotificationTrue()
        ));
    }
}