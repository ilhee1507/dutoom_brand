package com.dutoom.shop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String slug;
    private String name;
    private String category = "EEL";        // EEL(일반 상품) | GIFT
    private String brand = "eel";           // brands.code (관리자 > 브랜드)
    private String summary;
    private String description;
    private int price;
    private String weight;
    private String origin;
    private String storage;
    private String shippingInfo;
    private String imageUrl;
    private String gallery;
    private String features;
    private String process;
    private String cooking;
    private String packaging;
    private String faq;
    private int stock;
    private boolean active = true;
    private boolean featured;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sort")
    private List<ProductOption> options = new ArrayList<>();

    public List<String> getGalleryUrls() {
        return gallery == null ? List.of()
                : Arrays.stream(gallery.split("\\R")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** "Q: ..." 다음 줄 "A: ..." 형식을 질문/답변 쌍으로 */
    public List<String[]> getFaqItems() {
        List<String[]> out = new ArrayList<>();
        if (faq == null) return out;
        String q = null;
        for (String line : faq.split("\\R")) {
            line = line.trim();
            if (line.startsWith("Q:")) q = line.substring(2).trim();
            else if (line.startsWith("A:") && q != null) {
                out.add(new String[]{q, line.substring(2).trim()});
                q = null;
            }
        }
        return out;
    }

    public boolean isGift() { return "GIFT".equals(category); }

    public boolean isSoldOut() { return stock <= 0; }

    public String getPath() { return "/products/" + slug; }
}
