-- 멀티 브랜드: 상품이 속한 브랜드 (application.yml shop.brands[].code). 기존 상품은 모두 장어(eel)
alter table products add column brand varchar(30) not null default 'eel';
create index idx_products_brand on products(brand, category);
