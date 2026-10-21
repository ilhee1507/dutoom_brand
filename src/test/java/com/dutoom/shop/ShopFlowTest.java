package com.dutoom.shop;

import com.dutoom.shop.domain.OrderStatus;
import com.dutoom.shop.domain.Product;
import com.dutoom.shop.domain.Repos.OrderRepo;
import com.dutoom.shop.domain.Repos.ProductRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 메인/상세/SEO 렌더링 + 비회원 주문→테스트결제→재고 차감 + 권한 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ProductRepo products;
    @Autowired OrderRepo orders;

    @Test
    void homeRendersAllSectionsWithDbPrice() throws Exception {
        Product sig = products.findBySlugAndActiveTrue("signature").orElseThrow();
        String price = String.format("%,d", sig.getPrice());
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("두툼한 한 점,")))
                .andExpect(content().string(containsString("BRAND STORY")))
                .andExpect(content().string(containsString("/images/logo.png")))
                .andExpect(content().string(containsString("SIGNATURE PRODUCT")))
                .andExpect(content().string(containsString(price)))
                .andExpect(content().string(containsString("GIFT COLLECTION")))
                .andExpect(content().string(containsString("DINING EXPERIENCE")))
                .andExpect(content().string(containsString("장어 덮밥 레시피")))
                .andExpect(content().string(containsString("\"@type\":\"Organization\"")))
                .andExpect(content().string(not(containsString("KURAGI"))));
    }

    @Test
    void brandHomeSwitchesThemeCopyAndProducts() throws Exception {
        mvc.perform(get("/").param("brand", "kimchi")).andExpect(status().isOk())
                .andExpect(content().string(containsString("두툼김치 | 프리미엄 김치 전문 브랜드")))
                .andExpect(content().string(containsString("og:site_name\" content=\"두툼김치\"")))   // 헤더/푸터도 브랜드
                .andExpect(content().string(containsString("class=\"logo-text\">두툼김치</span>")))
                .andExpect(content().string(not(containsString("프리미엄 장어"))))
                .andExpect(content().string(not(containsString("DUTOOM EEL"))))
                .andExpect(content().string(containsString("--champagne-gold:#d8a24a")))
                .andExpect(content().string(containsString("두툼 포기김치")))
                .andExpect(content().string(containsString("/products?brand=kimchi")))
                .andExpect(content().string(containsString("?brand=kimchi\"")))   // canonical
                .andExpect(content().string(not(containsString("두툼장어 시그니처"))))
                .andExpect(content().string(not(containsString("id=\"dining-title\""))));
        mvc.perform(get("/products").param("brand", "kimchi"))
                .andExpect(content().string(containsString("두툼 맛김치")))
                .andExpect(content().string(not(containsString("두툼장어 시그니처"))));
        mvc.perform(get("/products"))
                .andExpect(content().string(not(containsString("두툼 포기김치"))));
        mvc.perform(get("/").param("brand", "beef")).andExpect(status().isOk())
                .andExpect(content().string(containsString("두툼스테이크 | 스테이크·도시락 전문 브랜드")))
                .andExpect(content().string(containsString("미국산 꽃등심 스테이크")))
                .andExpect(content().string(containsString("소불고기 도시락")))
                .andExpect(content().string(not(containsString("두툼 포기김치"))));
        mvc.perform(get("/reviews").param("brand", "kimchi"))   // 고객 후기는 브랜드별
                .andExpect(content().string(containsString("시어머니 생신 선물로")))
                .andExpect(content().string(containsString("/reviews?brand=kimchi")))
                .andExpect(content().string(not(containsString("살이 정말 두툼하고"))));
        mvc.perform(get("/reviews"))
                .andExpect(content().string(containsString("살이 정말 두툼하고")))
                .andExpect(content().string(not(containsString("시어머니 생신 선물로"))));
        mvc.perform(get("/").param("brand", "nope")).andExpect(status().isOk())   // 없는 브랜드 → 기본
                .andExpect(content().string(containsString("두툼한 한 점,")));
    }

    @Test
    void adminAddsBrandAndItsHomeUsesIt() throws Exception {
        var admin = user("admin@test.local").roles("ADMIN");
        mvc.perform(multipart("/admin/brands").with(admin).with(csrf())
                        .param("isNew", "true").param("code", "doenjang").param("sort", "30").param("name", "두툼된장")
                        .param("productTitle", "전통 된장").param("heroTitle", "항아리에서 익힌,").param("primaryColor", "#5a3a1a"))
                .andExpect(redirectedUrl("/admin/brands/doenjang"));
        mvc.perform(get("/").param("brand", "doenjang")).andExpect(status().isOk())
                .andExpect(content().string(containsString("두툼된장 | 전통 된장 전문 브랜드")))
                .andExpect(content().string(containsString("--royal-burgundy:#5a3a1a")))
                .andExpect(content().string(containsString("class=\"logo-text\">두툼된장</span>")));
        mvc.perform(get("/admin/brands").with(admin)).andExpect(content().string(containsString("/?brand=doenjang")));
        mvc.perform(multipart("/admin/brands").with(admin).with(csrf())   // 코드 중복
                        .param("isNew", "true").param("code", "eel").param("name", "x").param("productTitle", "x"))
                .andExpect(redirectedUrl("/admin/brands/new"));
        mvc.perform(multipart("/admin/brands").with(admin).with(csrf())   // style 속성 주입 방지
                        .param("isNew", "true").param("code", "bad").param("name", "x").param("productTitle", "x").param("primaryColor", "red;}body{"))
                .andExpect(redirectedUrl("/admin/brands/new"));
        mvc.perform(get("/admin/brands").with(user("u@test.com").roles("USER"))).andExpect(status().isForbidden());
    }

    @Test
    void productDetailHasStructuredDataMatchingScreen() throws Exception {
        Product sig = products.findBySlugAndActiveTrue("signature").orElseThrow();
        mvc.perform(get("/products/signature")).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"@type\":\"Product\"")))
                .andExpect(content().string(containsString("\"price\":" + sig.getPrice())))
                .andExpect(content().string(containsString("BreadcrumbList")))
                .andExpect(content().string(containsString("mobile-buy")));
        mvc.perform(get("/guide/donburi-recipe")).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"@type\":\"Article\"")));
        mvc.perform(get("/products/nope")).andExpect(status().isNotFound());
    }

    @Test
    void sitemapAndRobots() throws Exception {
        mvc.perform(get("/sitemap.xml")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/products/signature")))
                .andExpect(content().string(containsString("/guide/grilling-guide")));
        mvc.perform(get("/robots.txt")).andExpect(content().string(containsString("Disallow: /admin")));
    }

    @Test
    void guestBuyNowCheckoutMockPayDecrementsStock() throws Exception {
        Product sig = products.findBySlugAndActiveTrue("signature").orElseThrow();
        Long optionId = sig.getOptions().get(0).getId();
        int stockBefore = sig.getStock();
        MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/cart/add").session(session).with(csrf())
                        .param("productId", sig.getId().toString()).param("optionId", optionId.toString())
                        .param("quantity", "2").param("action", "buy"))
                .andExpect(redirectedUrl("/checkout?mode=buy"));

        MvcResult r = mvc.perform(post("/checkout").param("mode", "buy").session(session).with(csrf())
                        .param("ordererName", "홍길동").param("ordererPhone", "010-1234-5678").param("ordererEmail", "g@test.com")
                        .param("recipientName", "홍길동").param("recipientPhone", "010-1234-5678")
                        .param("zipcode", "06000").param("address1", "서울시 강남구").param("guestPassword", "1234")
                        .param("giftMessage", "감사합니다").param("agree", "true"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String payUrl = r.getResponse().getRedirectedUrl();
        // PAYMENT_MODE=mock: 주문 즉시 결제 완료 처리 → 완료 화면
        assertThat(payUrl).startsWith("/order/").endsWith("/complete");
        String orderNo = payUrl.split("/")[2];

        var order = orders.findByOrderNo(orderNo).orElseThrow();
        assertThat(order.getTotalAmount()).isEqualTo(sig.getPrice() * 2 + order.getShippingFee());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        // 다른 세션에서는 주문 접근 불가
        mvc.perform(get("/order/" + orderNo)).andExpect(status().isNotFound());
        assertThat(products.findById(sig.getId()).orElseThrow().getStock()).isEqualTo(stockBefore - 2);

        // 비회원 주문 조회 (새 세션)
        MockHttpSession other = new MockHttpSession();
        mvc.perform(post("/order/lookup").session(other).with(csrf()).param("orderNo", orderNo).param("password", "wrong"))
                .andExpect(redirectedUrl("/order/lookup"));
        mvc.perform(post("/order/lookup").session(other).with(csrf()).param("orderNo", orderNo).param("password", "1234"))
                .andExpect(redirectedUrl("/order/" + orderNo));
        mvc.perform(get("/order/" + orderNo).session(other)).andExpect(status().isOk())
                .andExpect(content().string(containsString("감사합니다")));
    }

    @Test
    void adminRequiresAdminRole() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin").with(user("u@test.com").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/products").with(user("admin@test.local").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("두툼장어 시그니처")));
    }
}
