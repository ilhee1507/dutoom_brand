package com.dutoom.shop.web;

import com.dutoom.shop.config.ShopProperties;
import com.dutoom.shop.config.ShopProperties.Brand;
import com.dutoom.shop.domain.*;
import com.dutoom.shop.domain.Repos.*;
import com.dutoom.shop.service.Brands;
import com.dutoom.shop.service.Seo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.List;

/** 메인, 상품 목록/상세, 검색, 후기 */
@Controller
public class ShopController {
    private static final List<OrderStatus> PURCHASED =
            List.of(OrderStatus.PAID, OrderStatus.PREPARING, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final ProductRepo products;
    private final ArticleRepo articles;
    private final ReviewRepo reviews;
    private final OrderRepo orders;
    private final GlobalModel global;
    private final Seo seo;
    private final ShopProperties props;
    private final Brands brands;

    public ShopController(ProductRepo products, ArticleRepo articles, ReviewRepo reviews, OrderRepo orders,
                          GlobalModel global, Seo seo, ShopProperties props, Brands brands) {
        this.props = props;
        this.brands = brands;
        this.products = products;
        this.articles = articles;
        this.reviews = reviews;
        this.orders = orders;
        this.global = global;
        this.seo = seo;
    }

    /** 메인: ?brand=kimchi 처럼 브랜드별 테마·문구·상품. 없는 브랜드는 기본 브랜드 */
    @GetMapping("/")
    String home(@RequestParam(required = false) String brand, Model m, HttpServletRequest req) {
        Brand b = brand(brand, m, req);
        List<Product> items = products.findByBrandAndCategoryAndActiveTrueOrderByFeaturedDescIdAsc(b.code(), "EEL");
        m.addAttribute("signature", items.isEmpty() ? null : items.get(0));
        m.addAttribute("items", items);
        m.addAttribute("hasGifts", !products.findByBrandAndCategoryAndActiveTrueOrderByFeaturedDescIdAsc(b.code(), "GIFT").isEmpty());
        m.addAttribute("story", b.storySlug() == null ? null : articles.findBySlugAndPublishedTrue(b.storySlug()).orElse(null));
        m.addAttribute("guides", articles.findByTypeAndPublishedTrueOrderBySortAsc("GUIDE").stream().limit(3).toList());
        m.addAttribute("jsonLd", seo.home(b));
        return "index";
    }

    @GetMapping("/products")
    String eels(@RequestParam(required = false) String brand, Model m, HttpServletRequest req) {
        Brand b = brand(brand, m, req);
        return list(m, b.productTitle(), b.productEyebrow(), products.findByBrandAndCategoryAndActiveTrueOrderByFeaturedDescIdAsc(b.code(), "EEL"));
    }

    @GetMapping("/gifts")
    String gifts(@RequestParam(required = false) String brand, Model m, HttpServletRequest req) {
        Brand b = brand(brand, m, req);
        return list(m, "선물세트", "GIFT COLLECTION", products.findByBrandAndCategoryAndActiveTrueOrderByFeaturedDescIdAsc(b.code(), "GIFT"));
    }

    /** 브랜드 확정 + canonical 에 ?brand= 포함 (브랜드 페이지가 기본 브랜드 페이지로 합쳐지지 않게) */
    private Brand brand(String code, Model m, HttpServletRequest req) {
        Brand b = brands.get(code);
        String q = brands.query(b.code());
        m.addAttribute("brand", b);
        m.addAttribute("brandQuery", q);
        m.addAttribute("canonical", props.baseUrl().replaceAll("/$", "") + req.getRequestURI() + q);
        return b;
    }

    @GetMapping("/search")
    String search(@RequestParam(defaultValue = "") String q, Model m) {
        m.addAttribute("q", q);
        List<Product> found = StringUtils.hasText(q) ? products.findByActiveTrueAndNameContainingIgnoreCaseOrderByIdAsc(q.trim()) : List.of();
        m.addAttribute("noindex", true);
        return list(m, "검색", "SEARCH", found);
    }

    private String list(Model m, String title, String eyebrow, List<Product> items) {
        m.addAttribute("title", title);
        m.addAttribute("eyebrow", eyebrow);
        m.addAttribute("items", items);
        return "shop/list";
    }

    @GetMapping("/products/{slug}")
    String detail(@PathVariable String slug, Model m, Principal principal) {
        Product p = products.findBySlugAndActiveTrue(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Review> rs = reviews.findByProductIdOrderByIdDesc(p.getId());
        Member me = global.member(principal);
        m.addAttribute("p", p);
        m.addAttribute("brand", brands.get(p.getBrand()));
        m.addAttribute("brandQuery", brands.query(p.getBrand()));
        m.addAttribute("reviews", rs);
        m.addAttribute("avgRating", rs.stream().mapToInt(Review::getRating).average().orElse(0));
        m.addAttribute("canReview", me != null && !reviews.existsByProductIdAndMemberId(p.getId(), me.getId())
                && orders.hasPurchased(me.getId(), p.getId(), PURCHASED));
        m.addAttribute("jsonLd", seo.product(p, rs));
        m.addAttribute("ogImage", p.getImageUrl());
        return "shop/detail";
    }

    @PostMapping("/reviews/write/{productId}")
    String writeReview(@PathVariable Long productId, @RequestParam int rating, @RequestParam String content,
                       Principal principal, RedirectAttributes ra) {
        Product p = products.findById(productId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Member me = global.member(principal);
        String text = content == null ? "" : content.trim();
        if (me == null || !orders.hasPurchased(me.getId(), productId, PURCHASED)) {
            ra.addFlashAttribute("error", "구매하신 회원만 후기를 작성할 수 있습니다.");
        } else if (reviews.existsByProductIdAndMemberId(productId, me.getId())) {
            ra.addFlashAttribute("error", "이미 후기를 작성하셨습니다.");
        } else if (rating < 1 || rating > 5 || text.length() < 5 || text.length() > 2000) {
            ra.addFlashAttribute("error", "별점과 5자 이상의 내용을 입력해 주세요.");
        } else {
            Review r = new Review();
            r.setProduct(p);
            r.setMemberId(me.getId());
            r.setAuthorName(me.getName());
            r.setRating(rating);
            r.setContent(text);
            reviews.save(r);
            ra.addFlashAttribute("message", "후기가 등록되었습니다.");
        }
        return "redirect:" + p.getPath() + "#reviews";
    }

    @GetMapping("/reviews")
    String allReviews(@RequestParam(required = false) String brand, Model m, HttpServletRequest req) {
        Brand b = brand(brand, m, req);   // 브랜드별 후기만 (없으면 기본 브랜드). 전체 후기는 관리자 화면
        m.addAttribute("reviews", reviews.findByProductBrandOrderByIdDesc(b.code()));
        return "shop/reviews";
    }
}
