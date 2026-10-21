-- [샘플 데이터] local/test 프로필에서만 적용된다 (운영 DB에는 들어가지 않음).
-- 가격·중량·원산지 등은 화면 확인용 예시이며 실제 판매 정보가 아니다.
insert into products (slug, name, category, summary, description, price, weight, origin, storage, shipping_info,
  image_url, gallery, features, process, cooking, packaging, faq, stock, featured) values
('signature', '두툼장어 시그니처', 'EEL',
 '까다롭게 선별한 최상급 장어로 특별한 식탁을 완성합니다.',
 '[샘플] 실제 상품 설명으로 교체하세요.',
 89000, '1kg (손질 후 700g 내외) [샘플]', '[관리자 입력 필요]', '냉동 보관 (-18℃ 이하) [샘플]',
 '[샘플] 결제 완료 후 영업일 기준 1~2일 내 출고',
 '/images/signature.jpg', '/images/hero-2.jpg',
 '[샘플] 상품 특징을 입력하세요.', '[샘플] 손질 및 품질관리 과정을 입력하세요.',
 '[샘플] 조리 방법을 입력하세요.', '[샘플] 배송 및 포장 정보를 입력하세요.',
 'Q: [샘플] 해동은 어떻게 하나요?
A: [샘플] 실제 답변으로 교체하세요.', 50, true),
('royal-gift', '두툼장어 로열 선물세트', 'GIFT',
 '깊은 풍미와 정성을 담아 마음을 전하는 특별한 선물.',
 '[샘플] 실제 선물세트 구성으로 교체하세요.',
 159000, '[샘플] 구성 입력 필요', '[관리자 입력 필요]', '냉동 보관 (-18℃ 이하) [샘플]',
 '[샘플] 선물 배송 안내',
 '/images/gift.jpg', null, null, null, null, null, null, 20, false);

insert into product_options (product_id, name, extra_price, sort)
select id, '기본 (소스 포함) [샘플]', 0, 0 from products where slug = 'signature';
insert into product_options (product_id, name, extra_price, sort)
select id, '소스 추가 [샘플]', 3000, 1 from products where slug = 'signature';
