package com.dutoom.shop.service;

import com.dutoom.shop.config.ShopProperties;
import com.dutoom.shop.domain.*;
import com.dutoom.shop.domain.Repos.OrderRepo;
import com.dutoom.shop.domain.Repos.ProductRepo;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private static final SecureRandom RND = new SecureRandom();

    private final ProductRepo products;
    private final OrderRepo orders;
    private final ShopProperties props;
    private final PasswordEncoder encoder;
    private final RestClient toss = RestClient.create("https://api.tosspayments.com");

    public OrderService(ProductRepo products, OrderRepo orders, ShopProperties props, PasswordEncoder encoder) {
        this.products = products;
        this.orders = orders;
        this.props = props;
        this.encoder = encoder;
    }

    /** 장바구니 라인 → 화면/주문용 라인. 판매중지·삭제·옵션 불일치 라인은 제외. */
    public record PricedLine(int index, Product product, ProductOption option, int quantity) {
        public int unitPrice() { return product.getPrice() + (option == null ? 0 : option.getExtraPrice()); }
        public int lineTotal() { return unitPrice() * quantity; }
    }

    public List<PricedLine> price(List<Cart.Line> lines) {
        List<PricedLine> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Cart.Line l = lines.get(i);
            Product p = products.findById(l.productId()).filter(Product::isActive).orElse(null);
            if (p == null) continue;
            ProductOption opt = null;
            if (l.optionId() != null) {
                opt = p.getOptions().stream().filter(o -> o.getId().equals(l.optionId())).findFirst().orElse(null);
                if (opt == null) continue;
            } else if (!p.getOptions().isEmpty()) {
                continue; // 옵션 필수 상품인데 옵션 미선택
            }
            out.add(new PricedLine(i, p, opt, l.quantity()));
        }
        return out;
    }

    public int itemsAmount(List<PricedLine> lines) {
        return lines.stream().mapToInt(PricedLine::lineTotal).sum();
    }

    @Transactional
    public Order place(CheckoutForm f, List<PricedLine> lines, Long memberId) {
        if (lines.isEmpty()) throw new IllegalStateException("주문할 상품이 없습니다.");
        for (PricedLine l : lines)
            if (l.product().getStock() < l.quantity())
                throw new IllegalStateException(l.product().getName() + " 재고가 부족합니다.");
        if (memberId == null && (f.getGuestPassword() == null || f.getGuestPassword().length() < 4))
            throw new IllegalStateException("비회원 주문 조회 비밀번호를 4자 이상 입력해 주세요.");

        Order o = new Order();
        o.setOrderNo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + String.format("%04d", RND.nextInt(10000)));
        o.setMemberId(memberId);
        if (memberId == null) o.setGuestPassword(encoder.encode(f.getGuestPassword()));
        o.setOrdererName(f.getOrdererName());
        o.setOrdererPhone(f.getOrdererPhone());
        o.setOrdererEmail(f.getOrdererEmail());
        o.setRecipientName(f.getRecipientName());
        o.setRecipientPhone(f.getRecipientPhone());
        o.setZipcode(f.getZipcode());
        o.setAddress1(f.getAddress1());
        o.setAddress2(f.getAddress2());
        o.setMemo(f.getMemo());
        o.setGiftMessage(f.getGiftMessage());
        for (PricedLine l : lines) {
            OrderItem it = new OrderItem();
            it.setOrder(o);
            it.setProductId(l.product().getId());
            it.setProductName(l.product().getName());
            it.setOptionName(l.option() == null ? null : l.option().getName());
            it.setUnitPrice(l.unitPrice());
            it.setQuantity(l.quantity());
            o.getItems().add(it);
        }
        int items = itemsAmount(lines);
        o.setItemsAmount(items);
        o.setShippingFee(props.shippingFeeFor(items));
        o.setTotalAmount(items + o.getShippingFee());
        return orders.save(o);
    }

    /**
     * 결제 승인 확정: 재고 차감 + 상태 변경. 이미 결제된 주문이면 그대로 반환(새로고침 대비).
     * confirmToss 내부 호출(프록시 우회)에서도 반영되도록 명시적으로 save 한다.
     * ponytail: 재고 동시성은 트랜잭션 단위 검사만 함. 동시 주문이 많아지면 비관적 락/조건부 update로.
     */
    @Transactional
    public Order markPaid(String orderNo, String method, String paymentKey) {
        Order o = orders.findByOrderNo(orderNo).orElseThrow();
        if (o.getStatus() != OrderStatus.PENDING_PAYMENT) return o;
        for (OrderItem it : o.getItems()) {
            Product p = products.findById(it.getProductId()).orElseThrow();
            if (p.getStock() < it.getQuantity()) throw new IllegalStateException(p.getName() + " 재고가 부족합니다.");
            p.setStock(p.getStock() - it.getQuantity());
            products.save(p);
        }
        o.setStatus(OrderStatus.PAID);
        o.setPaymentMethod(method);
        o.setPaymentKey(paymentKey);
        o.setPaidAt(LocalDateTime.now());
        o.setUpdatedAt(LocalDateTime.now());
        return orders.save(o);
    }

    /** 토스페이먼츠 결제 승인 API. 금액 위변조 방지를 위해 DB 금액과 비교 후 승인한다. */
    public Order confirmToss(String paymentKey, String orderNo, int amount) {
        Order o = orders.findByOrderNo(orderNo).orElseThrow();
        if (o.getStatus() != OrderStatus.PENDING_PAYMENT) return o;
        if (o.getTotalAmount() != amount) throw new IllegalStateException("결제 금액이 주문 금액과 다릅니다.");
        for (OrderItem it : o.getItems()) {
            Product p = products.findById(it.getProductId()).orElseThrow();
            if (p.getStock() < it.getQuantity()) throw new IllegalStateException(p.getName() + " 재고가 부족합니다.");
        }
        String auth = Base64.getEncoder().encodeToString((props.payment().tossSecretKey() + ":").getBytes(StandardCharsets.UTF_8));
        try {
            Map<?, ?> res = toss.post().uri("/v1/payments/confirm")
                    .header("Authorization", "Basic " + auth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("paymentKey", paymentKey, "orderId", orderNo, "amount", amount))
                    .retrieve().body(Map.class);
            String method = res == null ? "TOSS" : String.valueOf(res.get("method"));
            return markPaid(orderNo, method, paymentKey);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("결제 승인에 실패했습니다: " + e.getResponseBodyAsString());
        }
    }

    public boolean checkGuest(Order o, String password) {
        return o.getMemberId() == null && o.getGuestPassword() != null && password != null
                && encoder.matches(password, o.getGuestPassword());
    }

    @Transactional
    public void cancelPending(String orderNo) {
        orders.findByOrderNo(orderNo).filter(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT).ifPresent(o -> {
            o.setStatus(OrderStatus.CANCELLED);
            o.setUpdatedAt(LocalDateTime.now());
        });
    }
}
