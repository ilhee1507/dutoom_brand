package com.dutoom.shop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
@Getter @Setter
public class Article {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String slug;
    private String type;
    private String title;
    private String summary;
    private String body;
    private String imageUrl;
    private int sort;
    private boolean published = true;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static final String MAIN_STORY = "brand-story";

    /** 대표 브랜드 스토리(/story 고정 페이지, 메인 노출). 주소 변경·삭제 불가 */
    public boolean isMainStory() { return MAIN_STORY.equals(slug); }

    public String getPath() {
        if (isMainStory()) return "/story";
        return ("STORY".equals(type) ? "/story/" : "/guide/") + slug;
    }
}
