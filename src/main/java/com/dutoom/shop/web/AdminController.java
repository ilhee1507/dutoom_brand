package com.dutoom.shop.web;

import com.dutoom.shop.config.ShopProperties;
import com.dutoom.shop.domain.*;
import com.dutoom.shop.domain.Repos.*;
import com.dutoom.shop.service.Brands;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 관리자: 상품, 브랜드, 주문/배송, 콘텐츠(브랜드 스토리·가이드), 문의 답변·삭제, 후기 삭제. 상품은 주문이 참조하므로 삭제 대신 판매중지. */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp");

    private final ProductRepo products;
    private final OrderRepo orders;
    private final ArticleRepo articles;
    private final InquiryRepo inquiries;
    private final ReviewRepo reviews;
    private final BrandRepo brandRepo;
    private final Brands brands;
    private final ShopProperties props;

    public AdminController(ProductRepo products, OrderRepo orders, ArticleRepo articles, InquiryRepo inquiries,
                           ReviewRepo reviews, BrandRepo brandRepo, Brands brands, ShopProperties props) {
        this.brandRepo = brandRepo;
        this.brands = brands;
        this.products = products;
        this.orders = orders;
        this.articles = articles;
        this.inquiries = inquiries;
        this.reviews = reviews;
        this.props = props;
    }

    @ModelAttribute("noindex")
    boolean noindex() { return true; }

    @GetMapping
    String dashboard(Model m) {
        m.addAttribute("pending", orders.findByStatusOrderByIdDesc(OrderStatus.PAID).size());
        m.addAttribute("productCount", products.count());
        m.addAttribute("openInquiries", inquiries.findAllByOrderByIdDesc().stream().filter(q -> q.getAnswer() == null).count());
        return "admin/dashboard";
    }

    // ---------- 상품 ----------

    @GetMapping("/products")
    String products(Model m) {
        m.addAttribute("items", products.findAllByOrderByIdDesc());
        return "admin/products";
    }

    @GetMapping("/products/new")
    String newProduct(Model m) {
        m.addAttribute("p", new Product());
        m.addAttribute("optionsText", "");
        return "admin/product-form";
    }

    @GetMapping("/products/{id}")
    String editProduct(@PathVariable Long id, Model m) {
        Product p = products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        m.addAttribute("p", p);
        StringBuilder sb = new StringBuilder();
        for (ProductOption o : p.getOptions()) sb.append(o.getName()).append(" | ").append(o.getExtraPrice()).append('\n');
        m.addAttribute("optionsText", sb.toString());
        return "admin/product-form";
    }

    @PostMapping("/products")
    @Transactional
    String saveProduct(@ModelAttribute("form") Product form, @RequestParam(required = false) Long id,
                       @RequestParam(defaultValue = "") String optionsText,
                       @RequestParam(required = false) MultipartFile image, RedirectAttributes ra) throws IOException {
        Product p = id == null ? new Product() : products.findById(id).orElseThrow();
        if (!StringUtils.hasText(form.getName()) || !StringUtils.hasText(form.getSlug())
                || !form.getSlug().matches("[a-z0-9-]{2,80}") || form.getPrice() < 0 || form.getStock() < 0) {
            ra.addFlashAttribute("error", "상품명, 주소(slug: 영문 소문자·숫자·-), 가격, 재고를 확인해 주세요.");
            return "redirect:/admin/products" + (id == null ? "/new" : "/" + id);
        }
        p.setSlug(form.getSlug());
        p.setName(form.getName());
        p.setCategory("GIFT".equals(form.getCategory()) ? "GIFT" : "EEL");
        p.setBrand(brands.get(form.getBrand()).code());
        p.setSummary(form.getSummary());
        p.setDescription(form.getDescription());
        p.setPrice(form.getPrice());
        p.setWeight(form.getWeight());
        p.setOrigin(form.getOrigin());
        p.setStorage(form.getStorage());
        p.setShippingInfo(form.getShippingInfo());
        p.setGallery(form.getGallery());
        p.setFeatures(form.getFeatures());
        p.setProcess(form.getProcess());
        p.setCooking(form.getCooking());
        p.setPackaging(form.getPackaging());
        p.setFaq(form.getFaq());
        p.setStock(form.getStock());
        p.setActive(form.isActive());
        p.setFeatured(form.isFeatured());
        if (StringUtils.hasText(form.getImageUrl())) p.setImageUrl(form.getImageUrl());
        String uploaded = store(image);
        if (uploaded != null) p.setImageUrl(uploaded);
        p.setUpdatedAt(LocalDateTime.now());

        // 옵션: "이름 | 추가금액" 줄 단위. 기존 주문은 옵션명을 복사해 두므로 옵션 재작성이 주문 데이터에 영향 없음.
        p.getOptions().clear();
        int sort = 0;
        for (String line : optionsText == null ? new String[0] : optionsText.split("\\R")) {   // 빈 입력은 GlobalModel 바인더가 null 로 바꾼다
            if (line.isBlank()) continue;
            String[] parts = line.split("\\|");
            ProductOption o = new ProductOption();
            o.setProduct(p);
            o.setName(parts[0].trim());
            o.setExtraPrice(parts.length > 1 ? parseInt(parts[1]) : 0);
            o.setSort(sort++);
            p.getOptions().add(o);
        }
        products.save(p);
        ra.addFlashAttribute("message", "저장되었습니다.");
        return "redirect:/admin/products/" + p.getId();
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 이미지 업로드: 확장자·크기 제한, 파일명은 UUID로 교체 */
    private String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!IMAGE_EXT.contains(ext) || file.getContentType() == null || !file.getContentType().startsWith("image/"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jpg, png, webp 이미지만 업로드할 수 있습니다.");
        Path dir = Path.of(props.uploadDir()).toAbsolutePath();
        Files.createDirectories(dir);
        String stored = UUID.randomUUID() + "." + ext;
        file.transferTo(dir.resolve(stored));
        return "/uploads/" + stored;
    }

    // ---------- 브랜드 ----------

    @GetMapping("/brands")
    String brandList(Model m) {
        m.addAttribute("items", brandRepo.findAllByOrderBySortAscCodeAsc());
        return "admin/brands";
    }

    @GetMapping("/brands/new")
    String newBrand(Model m) {
        BrandInfo b = new BrandInfo();
        b.setSort((int) brandRepo.count() * 10);
        m.addAttribute("b", b);
        m.addAttribute("isNew", true);
        return "admin/brand-form";
    }

    @GetMapping("/brands/{code}")
    String editBrand(@PathVariable String code, Model m) {
        m.addAttribute("b", brandRepo.findById(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        m.addAttribute("isNew", false);
        return "admin/brand-form";
    }

    /** 폼이 모든 칸을 보내므로 그대로 저장. 코드는 상품·주소(?brand=)가 참조하므로 생성 후 변경 불가 */
    @PostMapping("/brands")
    @Transactional
    String saveBrand(@ModelAttribute("form") BrandInfo form, @RequestParam(defaultValue = "false") boolean isNew,
                     MultipartRequest files, RedirectAttributes ra) throws IOException {
        String code = form.getCode() == null ? "" : form.getCode();
        String back = "redirect:/admin/brands" + (isNew ? "/new" : "/" + code);
        if (isNew && (!code.matches("[a-z0-9-]{2,30}") || brandRepo.existsById(code))) {
            ra.addFlashAttribute("error", "브랜드 코드는 영문 소문자·숫자·- 2~30자이며 기존 코드와 겹치면 안 됩니다.");
            return back;
        }
        if (!isNew && !brandRepo.existsById(code)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!StringUtils.hasText(form.getName()) || !StringUtils.hasText(form.getProductTitle())
                || !isColor(form.getPrimaryColor()) || !isColor(form.getAccentColor())) {
            ra.addFlashAttribute("error", "브랜드명·메뉴명을 입력하고, 색상은 #7a1e1e 형식으로 입력해 주세요.");
            return back;
        }
        form.setLogo(withUpload(form.getLogo(), files.getFile("logoFile"), form.getName()));
        form.setStoryImage(withUpload(form.getStoryImage(), files.getFile("storyFile"), form.getName()));
        form.setNatureImage(withUpload(form.getNatureImage(), files.getFile("natureFile"), form.getName()));
        form.setSignatureImage(withUpload(form.getSignatureImage(), files.getFile("signatureFile"), form.getName()));
        form.setGiftImage(withUpload(form.getGiftImage(), files.getFile("giftFile"), form.getName()));
        String hero = store(files.getFile("heroFile"));   // 메인 배너는 업로드하면 한 줄 추가
        if (hero != null) form.setHeroImages((form.getHeroImages() == null ? "" : form.getHeroImages() + "\n") + hero + " | " + form.getName());
        brandRepo.save(form);
        brands.reload();
        ra.addFlashAttribute("message", "저장되었습니다.");
        return "redirect:/admin/brands/" + code;
    }

    /** 색상은 style 속성에 들어가므로 #hex 만 허용 (빈 값은 기본 색상) */
    private static boolean isColor(String c) { return c == null || c.matches("#[0-9a-fA-F]{3,8}"); }

    /** 업로드가 있으면 "경로 | 대체텍스트" 의 경로만 교체 (대체텍스트가 없으면 브랜드명) */
    private String withUpload(String field, MultipartFile file, String name) throws IOException {
        String url = store(file);
        if (url == null) return field;
        String alt = field != null && field.contains("|") ? field.substring(field.indexOf('|') + 1).trim() : name;
        return url + " | " + alt;
    }

    // ---------- 주문 ----------

    @GetMapping("/orders")
    String orders(@RequestParam(required = false) OrderStatus status, Model m) {
        m.addAttribute("items", status == null ? orders.findAllByOrderByIdDesc() : orders.findByStatusOrderByIdDesc(status));
        m.addAttribute("status", status);
        m.addAttribute("statuses", OrderStatus.values());
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    String order(@PathVariable Long id, Model m) {
        m.addAttribute("o", orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        m.addAttribute("statuses", OrderStatus.values());
        return "admin/order";
    }

    /** 상태·송장만 변경. 금액/품목 등 주문 원장은 수정하지 않는다. */
    @PostMapping("/orders/{id}")
    @Transactional
    String updateOrder(@PathVariable Long id, @RequestParam OrderStatus status, @RequestParam(required = false) String carrier,
                       @RequestParam(required = false) String trackingNo, RedirectAttributes ra) {
        Order o = orders.findById(id).orElseThrow();
        if (o.getStatus() == OrderStatus.PENDING_PAYMENT && status != OrderStatus.CANCELLED && status != OrderStatus.PENDING_PAYMENT) {
            ra.addFlashAttribute("error", "결제 대기 주문은 결제 완료 전까지 배송 상태로 변경할 수 없습니다.");
            return "redirect:/admin/orders/" + id;
        }
        o.setStatus(status);
        o.setCarrier(carrier);
        o.setTrackingNo(trackingNo);
        o.setUpdatedAt(LocalDateTime.now());
        ra.addFlashAttribute("message", "주문이 업데이트되었습니다." + (status == OrderStatus.CANCELLED && o.getPaidAt() != null
                ? " 결제 취소(환불)는 PG 관리자에서 별도로 처리해 주세요." : ""));
        return "redirect:/admin/orders/" + id;
    }

    // ---------- 콘텐츠 ----------

    @GetMapping("/articles")
    String articles(Model m) {
        m.addAttribute("items", articles.findAllByOrderBySortAsc());
        return "admin/articles";
    }

    @GetMapping("/articles/new")
    String newArticle(@RequestParam(defaultValue = "GUIDE") String type, Model m) {
        Article a = new Article();
        a.setType("STORY".equals(type) ? "STORY" : "GUIDE");
        a.setSort(articles.findAllByOrderBySortAsc().stream().mapToInt(Article::getSort).max().orElse(0) + 1);
        m.addAttribute("a", a);
        return "admin/article-form";
    }

    @GetMapping("/articles/{id}")
    String article(@PathVariable Long id, Model m) {
        m.addAttribute("a", articles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "admin/article-form";
    }

    /** 신규(id 없음)는 type(STORY/GUIDE)으로 생성. 대표 브랜드 스토리는 /story 고정 페이지라 주소 변경·삭제 불가 */
    @PostMapping("/articles")
    @Transactional
    String saveArticle(@RequestParam(required = false) Long id, @RequestParam(defaultValue = "GUIDE") String type,
                       @RequestParam(required = false) String slug,
                       @RequestParam String title, @RequestParam(required = false) String summary,
                       @RequestParam(required = false) String body, @RequestParam(required = false) String imageUrl,
                       @RequestParam(defaultValue = "0") int sort, @RequestParam(defaultValue = "false") boolean published,
                       @RequestParam(required = false) MultipartFile image, RedirectAttributes ra) throws IOException {
        Article a = id == null ? new Article() : articles.findById(id).orElseThrow();
        if (id == null) a.setType("STORY".equals(type) ? "STORY" : "GUIDE");
        String back = "redirect:/admin/articles/" + (id == null ? "new?type=" + a.getType() : id);
        if (!StringUtils.hasText(title)) {
            ra.addFlashAttribute("error", "제목을 입력해 주세요.");
            return back;
        }
        if (!a.isMainStory()) {
            if (slug == null || !slug.matches("[a-z0-9-]{2,80}")) {
                ra.addFlashAttribute("error", "주소(slug)는 영문 소문자·숫자·- 2~80자로 입력해 주세요.");
                return back;
            }
            if (articles.findBySlug(slug).filter(o -> !o.getId().equals(a.getId())).isPresent()) {
                ra.addFlashAttribute("error", "이미 사용 중인 주소(slug)입니다.");
                return back;
            }
            a.setSlug(slug);
        }
        a.setSort(sort);
        a.setTitle(title);
        a.setSummary(summary);
        a.setBody(body);
        if (StringUtils.hasText(imageUrl)) a.setImageUrl(imageUrl);
        String uploaded = store(image);
        if (uploaded != null) a.setImageUrl(uploaded);
        a.setPublished(published);
        a.setUpdatedAt(LocalDateTime.now());
        articles.save(a);
        ra.addFlashAttribute("message", "저장되었습니다.");
        return "redirect:/admin/articles/" + a.getId();
    }

    @PostMapping("/articles/{id}/delete")
    @Transactional
    String deleteArticle(@PathVariable Long id, RedirectAttributes ra) {
        Article a = articles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (a.isMainStory()) {
            ra.addFlashAttribute("error", "대표 브랜드 스토리는 삭제할 수 없습니다. 내용을 수정하거나 비공개로 전환해 주세요.");
            return "redirect:/admin/articles";
        }
        articles.delete(a);
        ra.addFlashAttribute("message", "'" + a.getTitle() + "' 콘텐츠를 삭제했습니다.");
        return "redirect:/admin/articles";
    }

    // ---------- 문의 ----------

    @GetMapping("/inquiries")
    String inquiries(Model m) {
        m.addAttribute("items", inquiries.findAllByOrderByIdDesc());
        return "admin/inquiries";
    }

    @PostMapping("/inquiries/{id}")
    @Transactional
    String answer(@PathVariable Long id, @RequestParam String answer, RedirectAttributes ra) {
        Inquiry q = inquiries.findById(id).orElseThrow();
        q.setAnswer(answer);
        q.setAnsweredAt(LocalDateTime.now());
        ra.addFlashAttribute("message", "답변이 저장되었습니다.");
        return "redirect:/admin/inquiries";
    }

    @PostMapping("/inquiries/{id}/delete")
    @Transactional
    String deleteInquiry(@PathVariable Long id, RedirectAttributes ra) {
        Inquiry q = inquiries.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        inquiries.delete(q);
        ra.addFlashAttribute("message", "'" + q.getTitle() + "' 문의를 삭제했습니다.");
        return "redirect:/admin/inquiries";
    }

    // ---------- 후기 ----------

    @GetMapping("/reviews")
    String reviews(Model m) {
        m.addAttribute("items", reviews.findAllByOrderByIdDesc());
        return "admin/reviews";
    }

    /** 삭제하면 같은 회원이 해당 상품에 후기를 다시 작성할 수 있다 (상품·회원당 1건 제약) */
    @PostMapping("/reviews/{id}/delete")
    @Transactional
    String deleteReview(@PathVariable Long id, RedirectAttributes ra) {
        Review r = reviews.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        reviews.delete(r);
        ra.addFlashAttribute("message", r.getAuthorName() + "님의 후기를 삭제했습니다.");
        return "redirect:/admin/reviews";
    }
}
