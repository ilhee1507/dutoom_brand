package com.dutoom.shop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter @Setter
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;
    private Long productId;
    private String productName;
    private String optionName;
    private int unitPrice;
    private int quantity;

    public int getLineTotal() { return unitPrice * quantity; }
}
