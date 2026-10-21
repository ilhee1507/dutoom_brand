package com.dutoom.shop.service;

import com.dutoom.shop.config.ShopProperties;
import com.dutoom.shop.domain.Article;
import com.dutoom.shop.domain.Product;
import com.dutoom.shop.domain.Review;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.*;

/** JSON-LD 구조화 데이터. 화면에 실제로 표시되는 값만 넣는다. */
@Component
public class Seo {
    private final ShopProperties props;
    private final Brands brands;
    private final ObjectMapper json;

    public Seo(ShopProperties props, Brands brands, ObjectMapper json) {
        this.props = props;
        this.brands = brands;
        this.json = json;
    }

    public String url(String path) { return props.baseUrl().replaceAll("/$", "") + path; }

    public String home(ShopProperties.Brand b) {
        var c = props.company();
        Map<String, Object> org = new LinkedHashMap<>();
        org.put("@type", "Organization");
        org.put("@id", url("/#org"));
        org.put("name", b.name());
        if (brands.isDefault(b)) org.put("alternateName", "DUTOOM EEL");
        org.put("url", url("/"));
        org.put("slogan", b.heroTitle() + " " + b.heroAccent());
        if (StringUtils.hasText(c.name())) org.put("legalName", c.name());
        if (StringUtils.hasText(c.phone()))
            org.put("contactPoint", Map.of("@type", "ContactPoint", "telephone", c.phone(), "contactType", "customer service", "areaServed", "KR", "availableLanguage", "Korean"));
        if (StringUtils.hasText(c.address())) org.put("address", c.address());
        Map<String, Object> site = new LinkedHashMap<>();
        site.put("@type", "WebSite");
        site.put("name", b.name());
        site.put("url", url("/"));
        site.put("inLanguage", "ko-KR");
        site.put("publisher", Map.of("@id", url("/#org")));
        site.put("potentialAction", Map.of("@type", "SearchAction",
                "target", url("/search?q={search_term_string}"), "query-input", "required name=search_term_string"));
        return write(Map.of("@context", "https://schema.org", "@graph", List.of(org, site)));
    }

    public String product(Product p, List<Review> reviews) {
        Map<String, Object> prod = new LinkedHashMap<>();
        prod.put("@type", "Product");
        prod.put("name", p.getName());
        if (StringUtils.hasText(p.getSummary())) prod.put("description", p.getSummary());
        if (StringUtils.hasText(p.getImageUrl())) prod.put("image", abs(p.getImageUrl()));
        prod.put("sku", p.getSlug());
        prod.put("brand", Map.of("@type", "Brand", "name", brands.get(p.getBrand()).name()));
        prod.put("offers", Map.of("@type", "Offer", "url", url(p.getPath()), "priceCurrency", "KRW",
                "price", p.getPrice(),
                "availability", p.isSoldOut() ? "https://schema.org/OutOfStock" : "https://schema.org/InStock"));
        if (!reviews.isEmpty()) {
            double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
            prod.put("aggregateRating", Map.of("@type", "AggregateRating",
                    "ratingValue", Math.round(avg * 10) / 10.0, "reviewCount", reviews.size()));
        }
        String section = p.isGift() ? "선물세트" : brands.get(p.getBrand()).productTitle();
        String sectionPath = (p.isGift() ? "/gifts" : "/products") + brands.query(p.getBrand());
        return write(Map.of("@context", "https://schema.org",
                "@graph", List.of(prod, crumbs(section, sectionPath, p.getName(), p.getPath()))));
    }

    public String article(Article a) {
        Map<String, Object> art = new LinkedHashMap<>();
        art.put("@type", "Article");
        art.put("headline", a.getTitle().replace("\n", " "));
        if (StringUtils.hasText(a.getSummary())) art.put("description", a.getSummary().replace("\n", " "));
        if (StringUtils.hasText(a.getImageUrl())) art.put("image", abs(a.getImageUrl()));
        art.put("datePublished", iso(a.getCreatedAt()));
        art.put("dateModified", iso(a.getUpdatedAt()));
        art.put("author", Map.of("@type", "Organization", "name", "두툼장어"));
        art.put("publisher", Map.of("@type", "Organization", "name", "두툼장어"));
        art.put("mainEntityOfPage", url(a.getPath()));
        Object crumb = a.isMainStory() ? crumbs("브랜드 스토리", "/story", null, null)
                : "STORY".equals(a.getType()) ? crumbs("브랜드 스토리", "/story", a.getTitle(), a.getPath())
                : crumbs("맛있게 즐기는 법", "/guide", a.getTitle(), a.getPath());
        return write(Map.of("@context", "https://schema.org", "@graph", List.of(art, crumb)));
    }

    private Map<String, Object> crumbs(String n1, String p1, String n2, String p2) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of("@type", "ListItem", "position", 1, "name", "홈", "item", url("/")));
        items.add(Map.of("@type", "ListItem", "position", 2, "name", n1, "item", url(p1)));
        if (n2 != null) items.add(Map.of("@type", "ListItem", "position", 3, "name", n2, "item", url(p2)));
        return Map.of("@type", "BreadcrumbList", "itemListElement", items);
    }

    private String abs(String path) { return path.startsWith("http") ? path : url(path); }

    private static String iso(java.time.LocalDateTime t) {
        return t.atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime().toString();
    }

    private String write(Object o) {
        try {
            // </script> 조기 종료 방지
            return json.writeValueAsString(o).replace("</", "<\\/");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
