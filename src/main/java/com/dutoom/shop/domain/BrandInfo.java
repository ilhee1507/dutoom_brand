package com.dutoom.shop.domain;

import com.dutoom.shop.config.ShopProperties.Brand;
import com.dutoom.shop.config.ShopProperties.Image;
import com.dutoom.shop.config.ShopProperties.Item;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

/** 브랜드 설정 (관리자 > 브랜드). 화면에는 toView() 로 바꾼 ShopProperties.Brand 를 쓴다.
 *  이미지 칸은 "경로 | 대체텍스트", heroImages 는 줄마다 한 장, benefits 는 줄마다 "아이콘 | 제목 | 설명" */
@Entity
@Table(name = "brands")
@Getter @Setter
public class BrandInfo {
    @Id
    private String code;
    private int sort;
    private String name;
    private String logo;
    private String eyebrow;
    private String heroTitle;
    private String heroAccent;
    private String heroSub;
    private String heroImages;
    private String productTitle;
    private String productEyebrow;
    private String storySlug;
    private String storyImage;
    private String natureImage;
    private String natureCopy;
    private String signatureTitle;
    private String signatureText;
    private String signatureImage;
    private String giftImage;
    private String benefits;
    private boolean productGrid;
    private boolean guides;
    private String primaryColor;
    private String accentColor;

    public Brand toView() {
        List<Item> items = lines(benefits).stream().map(l -> l.split("\\|", 3))
                .map(p -> new Item(p[0].trim(), p.length > 1 ? p[1].trim() : "", p.length > 2 ? p[2].trim() : "", false)).toList();
        return new Brand(code, name, image(logo), nz(eyebrow), nz(heroTitle), nz(heroAccent), nz(heroSub),
                lines(heroImages).stream().map(BrandInfo::image).toList(), productTitle, nz(productEyebrow),
                storySlug, image(storyImage), image(natureImage), nz(natureCopy), nz(signatureTitle), nz(signatureText),
                image(signatureImage), image(giftImage), items.isEmpty() ? null : items, productGrid, guides, primaryColor, accentColor);
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static List<String> lines(String s) {
        return s == null ? List.of() : Arrays.stream(s.split("\\R")).map(String::trim).filter(l -> !l.isEmpty()).toList();
    }

    /** "경로 | 대체텍스트" → Image (비어 있으면 null) */
    static Image image(String s) {
        if (s == null || s.isBlank()) return null;
        String[] p = s.split("\\|", 2);
        return new Image(p[0].trim(), p.length > 1 ? p[1].trim() : "");
    }
}
