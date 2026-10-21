package com.dutoom.shop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter @Setter
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    private Product product;
    private Long memberId;
    private String authorName;
    private int rating;
    private String content;
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 홍길동 → 홍*동, 김철 → 김* */
    public String getMaskedName() {
        String n = authorName == null ? "" : authorName;
        if (n.length() <= 2) return n.isEmpty() ? "*" : n.charAt(0) + "*";
        return n.charAt(0) + "*".repeat(n.length() - 2) + n.charAt(n.length() - 1);
    }
}
