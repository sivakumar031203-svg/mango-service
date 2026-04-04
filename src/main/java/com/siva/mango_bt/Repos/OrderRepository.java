package com.siva.mango_bt.Repos;

import com.siva.mango_bt.Entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByCustomerEmail(String email);

    List<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status);

    // Seller-scoped queries (fix for OrderController seller isolation)
    List<Order> findBySeller_IdOrderByCreatedAtDesc(Long sellerId);

    long countByIsNewNotificationTrue();

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.isNewNotification = false WHERE o.isNewNotification = true")
    void markAllNotificationsRead();

    // Stats helpers
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PENDING'")
    long countPending();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'DELIVERED'")
    long countDelivered();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'PAID'")
    java.math.BigDecimal sumRevenue();
}