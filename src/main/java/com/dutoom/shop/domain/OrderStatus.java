package com.dutoom.shop.domain;

public enum OrderStatus {
    PENDING_PAYMENT("결제 대기"), PAID("결제 완료"), PREPARING("상품 준비중"),
    SHIPPED("배송중"), DELIVERED("배송 완료"), CANCELLED("주문 취소");

    private final String label;

    OrderStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
