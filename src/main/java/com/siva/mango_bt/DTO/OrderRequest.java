package com.siva.mango_bt.DTO;

import lombok.*;
import java.util.List;

@Data
public class OrderRequest {
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String deliveryAddress;
    private String landmark;
    private String pincode;
    private String city;
    private String orderNotes;
    private String paymentMethod;
    private List<CartItem> items;

    @Data
    public static class CartItem {
        private Long mangoId;
        private Integer quantity;
    }
}
