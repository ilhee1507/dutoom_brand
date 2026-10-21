package com.dutoom.shop.web;

import com.dutoom.shop.config.ShopProperties;
import com.dutoom.shop.domain.Member;
import com.dutoom.shop.domain.Repos.MemberRepo;
import com.dutoom.shop.service.Brands;
import com.dutoom.shop.service.Cart;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

/** 모든 화면 공통 모델 + 현재 회원 조회 헬퍼 */
@ControllerAdvice(basePackages = "com.dutoom.shop.web")
public class GlobalModel {
    private final ShopProperties props;
    private final Cart cart;
    private final MemberRepo members;
    private final Brands brands;

    public GlobalModel(ShopProperties props, Cart cart, MemberRepo members, Brands brands) {
        this.props = props;
        this.brands = brands;
        this.cart = cart;
        this.members = members;
    }

    /** 빈 문자열 입력은 null로 — 빈 상세 항목이 화면에 빈 섹션으로 노출되지 않게 */
    @InitBinder
    void trimBlanks(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(false) {
            @Override
            public void setAsText(String text) {
                setValue(text == null || text.isBlank() ? null : text);
            }
        });
    }

    @ModelAttribute("shop")
    ShopProperties shop() { return props; }

    @ModelAttribute("brands")
    Brands brands() { return brands; }

    /** 헤더/푸터용 현재 브랜드 (?brand=code, 없으면 기본). 메인·목록·상세는 컨트롤러가 덮어쓴다 */
    @ModelAttribute("brand")
    ShopProperties.Brand brand(@RequestParam(name = "brand", required = false) String code) { return brands.get(code); }

    @ModelAttribute("brandQuery")
    String brandQuery(@RequestParam(name = "brand", required = false) String code) { return brands.query(code); }

    @ModelAttribute("cartCount")
    int cartCount() { return cart.count(); }

    /** canonical: 쿼리스트링을 제외한 현재 경로 */
    @ModelAttribute("canonical")
    String canonical(HttpServletRequest req) {
        return props.baseUrl().replaceAll("/$", "") + req.getRequestURI();
    }

    public Member member(Principal p) {
        return p == null ? null : members.findByEmail(p.getName()).orElse(null);
    }
}
