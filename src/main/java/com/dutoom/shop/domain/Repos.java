package com.dutoom.shop.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** 모든 리포지토리를 한 곳에 둔다 (Spring Data는 중첩 인터페이스도 스캔한다). */
public final class Repos {
    private Repos() {}

    public interface MemberRepo extends JpaRepository<Member, Long> {
        Optional<Member> findByEmail(String email);
    }

    public interface ProductRepo extends JpaRepository<Product, Long> {
        Optional<Product> findBySlugAndActiveTrue(String slug);
        List<Product> findByActiveTrueOrderByFeaturedDescIdAsc();
        List<Product> findByActiveTrueAndNameContainingIgnoreCaseOrderByIdAsc(String q);
        List<Product> findByBrandAndCategoryAndActiveTrueOrderByFeaturedDescIdAsc(String brand, String category);
        List<Product> findAllByOrderByIdDesc();
    }

    public interface OrderRepo extends JpaRepository<Order, Long> {
        Optional<Order> findByOrderNo(String orderNo);
        List<Order> findByMemberIdOrderByIdDesc(Long memberId);
        List<Order> findAllByOrderByIdDesc();
        List<Order> findByStatusOrderByIdDesc(OrderStatus status);

        @Query("select count(i) > 0 from OrderItem i where i.order.memberId = ?1 and i.productId = ?2 and i.order.status in ?3")
        boolean hasPurchased(Long memberId, Long productId, List<OrderStatus> statuses);
    }

    public interface BrandRepo extends JpaRepository<BrandInfo, String> {
        List<BrandInfo> findAllByOrderBySortAscCodeAsc();
    }

    public interface ReviewRepo extends JpaRepository<Review, Long> {
        List<Review> findByProductIdOrderByIdDesc(Long productId);
        List<Review> findAllByOrderByIdDesc();
        List<Review> findByProductBrandOrderByIdDesc(String brand);
        boolean existsByProductIdAndMemberId(Long productId, Long memberId);
    }

    public interface InquiryRepo extends JpaRepository<Inquiry, Long> {
        List<Inquiry> findByMemberIdOrderByIdDesc(Long memberId);
        List<Inquiry> findAllByOrderByIdDesc();
    }

    public interface ArticleRepo extends JpaRepository<Article, Long> {
        Optional<Article> findBySlug(String slug);
        Optional<Article> findBySlugAndPublishedTrue(String slug);
        List<Article> findByTypeAndPublishedTrueOrderBySortAsc(String type);
        List<Article> findAllByOrderBySortAsc();
    }
}
