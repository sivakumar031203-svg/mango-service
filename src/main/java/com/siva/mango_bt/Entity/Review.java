package com.siva.mango_bt.Entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "reviews")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mango_id", nullable = false)
    private Mango mango;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;      // null = guest review

    private String reviewerName;
    private String reviewerPhone;

    @Column(nullable = false)
    private Integer rating;         // 1–5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private Boolean approved = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}