package com.dutoom.shop.web;

import com.dutoom.shop.domain.Article;
import com.dutoom.shop.domain.Inquiry;
import com.dutoom.shop.domain.Member;
import com.dutoom.shop.domain.Repos.*;
import com.dutoom.shop.domain.Product;
import com.dutoom.shop.service.Seo;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Set;

/** 브랜드 스토리, 가이드, 정책, 고객 문의, 회원가입/로그인, robots/sitemap */
@Controller
public class ContentController {
    private static final Set<String> POLICIES = Set.of("privacy", "terms", "shipping");

    private final ArticleRepo articles;
    private final InquiryRepo inquiries;
    private final MemberRepo members;
    private final ProductRepo products;
    private final PasswordEncoder encoder;
    private final GlobalModel global;
    private final Seo seo;

    public ContentController(ArticleRepo articles, InquiryRepo inquiries, MemberRepo members, ProductRepo products,
                             PasswordEncoder encoder, GlobalModel global, Seo seo) {
        this.articles = articles;
        this.inquiries = inquiries;
        this.members = members;
        this.products = products;
        this.encoder = encoder;
        this.global = global;
        this.seo = seo;
    }

    @GetMapping("/story")
    String story(Model m) {
        return article(articles.findBySlugAndPublishedTrue(Article.MAIN_STORY), m);
    }

    @GetMapping("/story/{slug}")
    String storyItem(@PathVariable String slug, Model m) {
        if (Article.MAIN_STORY.equals(slug)) return "redirect:/story";
        return article(articles.findBySlugAndPublishedTrue(slug).filter(a -> "STORY".equals(a.getType())), m);
    }

    @GetMapping("/guide")
    String guides(Model m) {
        m.addAttribute("guides", articles.findByTypeAndPublishedTrueOrderBySortAsc("GUIDE"));
        return "content/guides";
    }

    @GetMapping("/guide/{slug}")
    String guide(@PathVariable String slug, Model m) {
        return article(articles.findBySlugAndPublishedTrue(slug).filter(a -> "GUIDE".equals(a.getType())), m);
    }

    private String article(java.util.Optional<Article> a, Model m) {
        Article art = a.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        m.addAttribute("a", art);
        m.addAttribute("jsonLd", seo.article(art));
        if ("STORY".equals(art.getType()))  // 브랜드 이야기 더 보기: 같은 STORY 중 현재 글 제외
            m.addAttribute("stories", articles.findByTypeAndPublishedTrueOrderBySortAsc("STORY").stream()
                    .filter(o -> !o.getId().equals(art.getId())).toList());
        return "content/article";
    }

    @GetMapping("/policy/{name}")
    String policy(@PathVariable String name, Model m) {
        if (!POLICIES.contains(name)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        m.addAttribute("name", name);
        return "content/policy";
    }

    // ---------- 고객 문의 ----------

    @GetMapping("/inquiry")
    String inquiryForm(Model m, Principal principal) {
        m.addAttribute("me", global.member(principal));
        return "content/inquiry";
    }

    @PostMapping("/inquiry")
    String inquiry(@RequestParam String name, @RequestParam String email, @RequestParam String title,
                   @RequestParam String content, Principal principal, RedirectAttributes ra) {
        if (!StringUtils.hasText(name) || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
                || !StringUtils.hasText(title) || !StringUtils.hasText(content)
                || name.length() > 50 || email.length() > 120 || title.length() > 200 || content.length() > 4000) {
            ra.addFlashAttribute("error", "입력 내용을 확인해 주세요.");
            return "redirect:/inquiry";
        }
        Member me = global.member(principal);
        Inquiry q = new Inquiry();
        q.setMemberId(me == null ? null : me.getId());
        q.setName(name.trim());
        q.setEmail(email.trim());
        q.setTitle(title.trim());
        q.setContent(content.trim());
        inquiries.save(q);
        ra.addFlashAttribute("message", "문의가 접수되었습니다. 입력하신 이메일로 답변드리겠습니다.");
        return "redirect:/inquiry";
    }

    // ---------- 계정 ----------

    @GetMapping("/login")
    String login(Model m) {
        m.addAttribute("noindex", true);
        return "account/login";
    }

    @GetMapping("/signup")
    String signupForm(Model m) {
        m.addAttribute("noindex", true);
        return "account/signup";
    }

    @PostMapping("/signup")
    String signup(@RequestParam String email, @RequestParam String password, @RequestParam String name,
                  @RequestParam(required = false) String phone, @RequestParam(defaultValue = "false") boolean agree,
                  RedirectAttributes ra) {
        String e = email.trim().toLowerCase();
        String err = null;
        if (!e.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") || e.length() > 120) err = "이메일 형식을 확인해 주세요.";
        else if (password.length() < 8 || password.length() > 64) err = "비밀번호는 8자 이상이어야 합니다.";
        else if (!StringUtils.hasText(name) || name.length() > 50) err = "이름을 입력해 주세요.";
        else if (!agree) err = "이용약관 및 개인정보 수집·이용에 동의해 주세요.";
        else if (members.findByEmail(e).isPresent()) err = "이미 가입된 이메일입니다.";
        if (err != null) {
            ra.addFlashAttribute("error", err);
            return "redirect:/signup";
        }
        Member m = new Member();
        m.setEmail(e);
        m.setPassword(encoder.encode(password));
        m.setName(name.trim());
        m.setPhone(phone);
        members.save(m);
        ra.addFlashAttribute("message", "가입이 완료되었습니다. 로그인해 주세요.");
        return "redirect:/login";
    }

    // ---------- SEO ----------

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    String robots() {
        return """
                User-agent: *
                Allow: /
                Disallow: /admin
                Disallow: /cart
                Disallow: /checkout
                Disallow: /order
                Disallow: /mypage
                Disallow: /login
                Disallow: /signup
                Disallow: /search

                Sitemap: %s
                """.formatted(seo.url("/sitemap.xml"));
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    String sitemap() {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String p : new String[]{"/", "/products", "/gifts", "/guide", "/reviews", "/inquiry"}) url(sb, p, null);
        for (Product p : products.findByActiveTrueOrderByFeaturedDescIdAsc()) url(sb, p.getPath(), p.getUpdatedAt());
        for (Article a : articles.findAllByOrderBySortAsc()) if (a.isPublished()) url(sb, a.getPath(), a.getUpdatedAt());
        return sb.append("</urlset>\n").toString();
    }

    private void url(StringBuilder sb, String path, java.time.LocalDateTime mod) {
        sb.append("  <url><loc>").append(seo.url(path)).append("</loc>");
        if (mod != null) sb.append("<lastmod>").append(mod.toLocalDate()).append("</lastmod>");
        sb.append("</url>\n");
    }
}
