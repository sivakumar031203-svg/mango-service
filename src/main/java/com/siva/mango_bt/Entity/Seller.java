package com.siva.mango_bt.Entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "sellers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Seller {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String mobile;          // login ID

    @Column(nullable = false)
    private String password;        // BCrypt

    @Column(nullable = false)
    private String storeName;

    private String ownerName;
    private String email;
    private String whatsapp;
    private String city;

    @Column(columnDefinition = "TEXT")
    private String storeDescription;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean passwordChanged = false;  // false = still on default password

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}