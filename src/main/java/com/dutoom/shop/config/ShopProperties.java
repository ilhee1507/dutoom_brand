package com.dutoom.shop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** application.yml 의 shop.* — 운영 정보는 환경변수로 관리한다. */
@ConfigurationProperties("shop")
public record ShopProperties(
        String baseUrl,
        String uploadDir,
        int shippingFee,
        int freeShippingOver,
        Company company,
        Payment payment,
        Admin admin,
        List<Item> benefits,
        List<Item> trust) {

    public record Company(String name, String ceo, String businessNo, String mailOrderNo,
                          String address, String phone, String email, String csHours) {}

    public record Payment(String mode, String tossClientKey, String tossSecretKey) {
        public boolean isToss() { return "toss".equalsIgnoreCase(mode); }
    }

    public record Admin(String email, String password) {}

    public record Item(String icon, String title, String sub, boolean temp) {}

    public record Image(String src, String alt) {}

    /** 화면용 브랜드 정보 (brands 테이블 → BrandInfo.toView, service.Brands). 비어 있는 이미지/색상은 화면에서 생략·기본값 */
    public record Brand(String code, String name, Image logo, String eyebrow, String heroTitle, String heroAccent, String heroSub,
                        List<Image> heroImages, String productTitle, String productEyebrow,
                        String storySlug, Image storyImage, Image natureImage, String natureCopy,
                        String signatureTitle, String signatureText, Image signatureImage,
                        Image giftImage, List<Item> benefits, boolean productGrid, boolean guides, String primary, String accent) {
        public List<Image> heroImages() { return heroImages == null ? List.of() : heroImages; }

        /** 상단 메뉴 '브랜드 스토리' 링크. 대표 스토리(brand-story)는 /story 고정 */
        public String storyPath() {
            return storySlug == null || "brand-story".equals(storySlug) ? "/story" : "/story/" + storySlug;
        }

        /** 디자인 시스템 색상 변수 덮어쓰기 (main 요소 style) */
        public String theme() {
            String css = "";
            if (primary != null) css += "--royal-burgundy:" + primary + ";";
            if (accent != null) css += "--champagne-gold:" + accent + ";--gold-light:" + accent
                    + ";--gold-grad:linear-gradient(180deg," + accent + "," + accent + ");";
            return css;
        }
    }

    public int shippingFeeFor(int itemsAmount) {
        return itemsAmount == 0 || itemsAmount >= freeShippingOver ? 0 : shippingFee;
    }
}
