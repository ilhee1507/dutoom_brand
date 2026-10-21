package com.dutoom.shop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiries")
@Getter @Setter
public class Inquiry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    private String name;
    private String email;
    private String title;
    private String content;
    private String answer;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime answeredAt;
}
