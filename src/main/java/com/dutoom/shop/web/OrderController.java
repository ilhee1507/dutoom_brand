package com.dutoom.shop.web;

import com.dutoom.shop.config.ShopProperties;
import com.dutoom.shop.domain.Member;
import com.dutoom.shop.domain.Order;
import com.dutoom.shop.domain.OrderStatus;
import com.dutoom.shop.domain.Repos.InquiryRepo;
import com.dutoom.shop.domain.Repos.OrderRepo;
import com.dutoom.shop.service.Cart;
import com.dutoom.shop.service.CheckoutForm;
import com.dutoom.shop.service.OrderService;
import com.dutoom.shop.service.OrderService.PricedLine;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 장바구니 → 주문서 → 결제 → 완료, 주문 조회(회원/비회원), 마이페이지 */
@Controller
public class OrderController {
    private static final String GUEST_ORDERS = "guestOrders";

    private final Cart cart;
    private final OrderService service;
    private final OrderRepo orders;
    private final InquiryRepo inquiries;
    private final GlobalModel global;
    private final ShopProperties props;

    public OrderController(Cart cart, OrderService service, OrderRepo orders, InquiryRepo inquiries,
                           GlobalModel global, ShopProperties props) {
        this.cart = cart;
        this.service = service;
        this.orders = orders;
        this.inquiries = inquiries;
        this.global = global;
        this.props = props;
    }

    // ---------- 장바구니 ----------

    @GetMapping("/cart")
    String cart(Model m) {
        List<PricedLine> lines = service.price(cart.lines());
        addTotals(m, lines);
        m.addAttribute("noindex", true);
        return "order/cart";
    }

    @PostMapping("/cart/add")
    String add(@RequestParam Long productId, @RequestParam(required = false) Long optionId,
               @RequestParam(defaultValue = "1") int quantity, @RequestParam(defaultValue = "cart") String action,
               @RequestHeader(value = "Referer", required = false) String referer, RedirectAttributes ra) {
        int qty = Math.max(1, Math.min(99, quantity));
        var line = new Cart.Line(productId, optionId, qty);
        if (service.price(List.of(line)).isEmpty()) {
            ra.addFlashAttribute("error", "옵션을 선택해 주세요.");
            return "redirect:" + (referer != null && referer.startsWith(props.baseUrl()) ? referer : "/");
        }
        if ("buy".equals(action)) {
            cart.setBuyNow(List.of(line));
            return "redirect:/checkout?mode=buy";
        }
        cart.add(productId, optionId, qty);
        ra.addFlashAttribute("message", "장바구니에 담았습니다.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    String update(@RequestParam int index, @RequestParam int quantity) {
        cart.update(index, quantity);
        return "redirect:/cart";
    }

    // ---------- 주문서 ----------

    private List<PricedLine> checkoutLines(String mode) {
        return service.price("buy".equals(mode) ? cart.buyNow() : cart.lines());
    }

    @GetMapping("/checkout")
    String checkout(@RequestParam(defaultValue = "cart") String mode, Model m, Principal principal) {
        List<PricedLine> lines = checkoutLines(mode);
        if (lines.isEmpty()) return "redirect:/cart";
        CheckoutForm f = new CheckoutForm();
        Member me = global.member(principal);
        if (me != null) {
            f.setOrdererName(me.getName());
            f.setOrdererEmail(me.getEmail());
            f.setOrdererPhone(me.getPhone());
        }
        m.addAttribute("form", f);
        return checkoutView(m, mode, lines, me);
    }

    private String checkoutView(Model m, String mode, List<PricedLine> lines, Member me) {
        addTotals(m, lines);
        m.addAttribute("mode", mode);
        m.addAttribute("guest", me == null);
        m.addAttribute("noindex", true);
        return "order/checkout";
    }

    @PostMapping("/checkout")
    String place(@RequestParam(defaultValue = "cart") String mode, @Valid @ModelAttribute("form") CheckoutForm form,
                 BindingResult br, Model m, Principal principal, HttpSession session) {
        List<PricedLine> lines = checkoutLines(mode);
        if (lines.isEmpty()) return "redirect:/cart";
        Member me = global.member(principal);
        if (!form.isAgree()) br.reject("agree", "주문 내용 확인 및 결제 진행에 동의해 주세요.");
        if (me == null && (form.getGuestPassword() == null || form.getGuestPassword().length() < 4))
            br.rejectValue("guestPassword", "short", "비회원 주문조회 비밀번호를 4자 이상 입력해 주세요.");
        if (br.hasErrors()) return checkoutView(m, mode, lines, me);
        Order o;
        try {
            o = service.place(form, lines, me == null ? null : me.getId());
            // PG 미연동(mock): 결제 완료로 간주하고 바로 처리
            if (!props.payment().isToss()) service.markPaid(o.getOrderNo(), "TEST", "mock-" + o.getOrderNo());
        } catch (IllegalStateException e) {
            br.reject("order", e.getMessage());
            return checkoutView(m, mode, lines, me);
        }
        if ("buy".equals(mode)) cart.setBuyNow(List.of());
        else cart.clear();
        guestOrders(session).add(o.getOrderNo());
        return "redirect:/order/" + o.getOrderNo() + (props.payment().isToss() ? "/pay" : "/complete");
    }

    // ---------- 결제 ----------

    @GetMapping("/order/{orderNo}/pay")
    String pay(@PathVariable String orderNo, Model m, Principal principal, HttpSession session) {
        Order o = accessible(orderNo, principal, session);
        if (o.getStatus() != OrderStatus.PENDING_PAYMENT) return "redirect:/order/" + orderNo;
        m.addAttribute("order", o);
        m.addAttribute("noindex", true);
        return "order/pay";
    }

    /** PAYMENT_MODE=mock 일 때만 동작하는 테스트 결제 (PG 미설정 환경 검증용) */
    @PostMapping("/order/{orderNo}/mock-pay")
    String mockPay(@PathVariable String orderNo, Principal principal, HttpSession session, RedirectAttributes ra) {
        if (props.payment().isToss()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        accessible(orderNo, principal, session);
        try {
            service.markPaid(orderNo, "TEST", "mock-" + orderNo);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/order/" + orderNo + "/pay";
        }
        return "redirect:/order/" + orderNo + "/complete";
    }

    /** 토스페이먼츠 successUrl */
    @GetMapping("/payment/success")
    String tossSuccess(@RequestParam String paymentKey, @RequestParam String orderId, @RequestParam int amount,
                       Principal principal, HttpSession session, RedirectAttributes ra) {
        accessible(orderId, principal, session);
        try {
            service.confirmToss(paymentKey, orderId, amount);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/order/" + orderId + "/pay";
        }
        return "redirect:/order/" + orderId + "/complete";
    }

    /** 토스페이먼츠 failUrl */
    @GetMapping("/payment/fail")
    String tossFail(@RequestParam(required = false) String orderId, @RequestParam(required = false) String message,
                    RedirectAttributes ra) {
        ra.addFlashAttribute("error", "결제가 완료되지 않았습니다." + (message == null ? "" : " (" + message + ")"));
        return orderId == null ? "redirect:/cart" : "redirect:/order/" + orderId + "/pay";
    }

    @GetMapping("/order/{orderNo}/complete")
    String complete(@PathVariable String orderNo, Model m, Principal principal, HttpSession session) {
        m.addAttribute("order", accessible(orderNo, principal, session));
        m.addAttribute("noindex", true);
        return "order/complete";
    }

    @PostMapping("/order/{orderNo}/cancel")
    String cancel(@PathVariable String orderNo, Principal principal, HttpSession session) {
        accessible(orderNo, principal, session);
        service.cancelPending(orderNo);
        return "redirect:/order/" + orderNo;
    }

    // ---------- 주문 조회 ----------

    @GetMapping("/order/{orderNo}")
    String detail(@PathVariable String orderNo, Model m, Principal principal, HttpSession session) {
        m.addAttribute("order", accessible(orderNo, principal, session));
        m.addAttribute("noindex", true);
        return "order/detail";
    }

    @GetMapping("/order/lookup")
    String lookupForm(Model m) {
        m.addAttribute("noindex", true);
        return "order/lookup";
    }

    @PostMapping("/order/lookup")
    String lookup(@RequestParam String orderNo, @RequestParam String password, HttpSession session, RedirectAttributes ra) {
        Order o = orders.findByOrderNo(orderNo.trim()).orElse(null);
        if (o == null || !service.checkGuest(o, password)) {
            ra.addFlashAttribute("error", "주문번호 또는 비밀번호가 일치하지 않습니다.");
            return "redirect:/order/lookup";
        }
        guestOrders(session).add(o.getOrderNo());
        return "redirect:/order/" + o.getOrderNo();
    }

    @GetMapping("/mypage")
    String mypage(Model m, Principal principal) {
        Member me = global.member(principal);
        m.addAttribute("me", me);
        m.addAttribute("orders", orders.findByMemberIdOrderByIdDesc(me.getId()));
        m.addAttribute("inquiries", inquiries.findByMemberIdOrderByIdDesc(me.getId()));
        m.addAttribute("noindex", true);
        return "account/mypage";
    }

    // ---------- helpers ----------

    /** 주문 접근 권한: 본인 회원 주문, 관리자, 또는 이 세션에서 생성/조회 인증한 비회원 주문 */
    private Order accessible(String orderNo, Principal principal, HttpSession session) {
        Order o = orders.findByOrderNo(orderNo).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Member me = global.member(principal);
        boolean ok = (me != null && (o.getMemberId() != null && o.getMemberId().equals(me.getId()) || "ADMIN".equals(me.getRole())))
                || (o.getMemberId() == null && guestOrders(session).contains(orderNo));
        if (!ok) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return o;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> guestOrders(HttpSession session) {
        Set<String> s = (Set<String>) session.getAttribute(GUEST_ORDERS);
        if (s == null) {
            s = new HashSet<>();
            session.setAttribute(GUEST_ORDERS, s);
        }
        return s;
    }

    private void addTotals(Model m, List<PricedLine> lines) {
        int items = service.itemsAmount(lines);
        int fee = props.shippingFeeFor(items);
        m.addAttribute("lines", lines);
        m.addAttribute("itemsAmount", items);
        m.addAttribute("shippingFee", fee);
        m.addAttribute("totalAmount", items + fee);
    }
}
