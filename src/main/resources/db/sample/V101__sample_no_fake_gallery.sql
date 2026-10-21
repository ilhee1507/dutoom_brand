-- [샘플] '실제 상품 사진'에는 실제 판매 상품 사진만 둔다. 출처가 다른 참고 이미지는 제거.
update products set gallery = null where slug = 'signature' and gallery = '/images/hero-2.jpg';
