package com.dutoom.shop.service;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 세션 장바구니 — 회원/비회원 공통. 가격은 담지 않고 주문 시 DB에서 다시 계산한다.
 * ponytail: 세션 저장이라 로그아웃·세션 만료 시 비워짐. 기기 간 공유가 필요하면 DB 장바구니로 전환.
 */
@Component
@SessionScope
public class Cart implements Serializable {

    public record Line(Long productId, Long optionId, int quantity) implements Serializable {}

    private final List<Line> lines = new ArrayList<>();
    /** 바로 구매 시 장바구니와 분리해 보관 */
    private List<Line> buyNow = List.of();

    public synchronized void add(Long productId, Long optionId, int qty) {
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            if (l.productId().equals(productId) && Objects.equals(l.optionId(), optionId)) {
                lines.set(i, new Line(productId, optionId, Math.min(99, l.quantity() + qty)));
                return;
            }
        }
        lines.add(new Line(productId, optionId, qty));
    }

    public synchronized void update(int index, int qty) {
        if (index < 0 || index >= lines.size()) return;
        Line l = lines.get(index);
        if (qty <= 0) lines.remove(index);
        else lines.set(index, new Line(l.productId(), l.optionId(), Math.min(99, qty)));
    }

    public synchronized List<Line> lines() { return List.copyOf(lines); }

    public synchronized void clear() { lines.clear(); }

    public synchronized int count() { return lines.stream().mapToInt(Line::quantity).sum(); }

    public synchronized List<Line> buyNow() { return buyNow; }

    public synchronized void setBuyNow(List<Line> l) { buyNow = List.copyOf(l); }
}
