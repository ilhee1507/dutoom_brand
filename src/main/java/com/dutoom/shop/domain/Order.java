package com.dutoom.shop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderNo;
    private Long memberId;
    private String guestPassword;
    private String ordererName;
    private String ordererPhone;
    private String ordererEmail;
    private String recipientName;
    private String recipientPhone;
    private String zipcode;
    private String address1;
    private String address2;
    private String memo;
    private String giftMessage;
    private int itemsAmount;
    private int shippingFee;
    private int totalAmount;
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;
    private String paymentMethod;
    private String paymentKey;
    private LocalDateTime paidAt;
    private String carrier;
    private String trackingNo;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    public String getOrderName() {
        if (items.isEmpty()) return "두툼장어 주문";
        String first = items.get(0).getProductName();
        return items.size() == 1 ? first : first + " 외 " + (items.size() - 1) + "건";
    }
}
