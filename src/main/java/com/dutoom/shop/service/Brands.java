package com.dutoom.shop.service;

import com.dutoom.shop.config.ShopProperties.Brand;
import com.dutoom.shop.domain.BrandInfo;
import com.dutoom.shop.domain.Repos.BrandRepo;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

/** brands 테이블을 메모리에 들고 있는 브랜드 목록. 관리자 저장 후 reload(). 첫 번째(sort 최소)가 기본 브랜드 */
@Service
public class Brands {
    private final BrandRepo repo;
    private volatile List<Brand> all = List.of();

    public Brands(BrandRepo repo) { this.repo = repo; }

    @PostConstruct
    public void reload() {
        all = repo.findAllByOrderBySortAscCodeAsc().stream().map(BrandInfo::toView).toList();
    }

    public List<Brand> all() { return all; }

    /** 없는 코드·빈 값이면 기본 브랜드 */
    public Brand get(String code) {
        return all.stream().filter(b -> b.code().equals(code)).findFirst().orElse(all.get(0));
    }

    public boolean isDefault(Brand b) { return b == all.get(0); }

    /** 기본 브랜드는 "", 그 외 "?brand=code" — 목록/메인 링크와 canonical 에 붙인다 */
    public String query(String code) {
        Brand b = get(code);
        return isDefault(b) ? "" : "?brand=" + b.code();
    }
}
