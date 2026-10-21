-- [샘플] 멀티 브랜드 확인용 김치 브랜드 상품·스토리 (local/test 전용). /?brand=kimchi
insert into products (slug, name, brand, category, summary, description, price, weight, origin, storage, shipping_info,
  image_url, stock, featured) values
('kimchi-pogi', '두툼 포기김치', 'kimchi', 'EEL',
 '해남 배추를 천일염에 절여 깊은 맛으로 숙성한 포기김치.',
 '[샘플] 실제 상품 설명으로 교체하세요.',
 32000, '3kg [샘플]', '[관리자 입력 필요]', '냉장 보관 (0~5℃) [샘플]',
 '[샘플] 주문 후 담가 1~2일 내 출고', '/images/kimchi-hero.jpg', 40, true),
('kimchi-mat', '두툼 맛김치', 'kimchi', 'EEL',
 '한입 크기로 썰어 바로 먹기 좋은 맛김치.',
 '[샘플] 실제 상품 설명으로 교체하세요.',
 18000, '1.5kg [샘플]', '[관리자 입력 필요]', '냉장 보관 (0~5℃) [샘플]',
 '[샘플] 주문 후 1~2일 내 출고', '/images/kimchi-hero.jpg', 60, false),
('kimchi-gift', '두툼 김치 명절 선물세트', 'kimchi', 'GIFT',
 '포기김치와 총각김치를 옹기 패키지에 담았습니다.',
 '[샘플] 실제 선물세트 구성으로 교체하세요.',
 69000, '포기김치 3kg + 총각김치 1kg [샘플]', '[관리자 입력 필요]', '냉장 보관 (0~5℃) [샘플]',
 '[샘플] 선물 배송 안내', '/images/kimchi-story.jpg', 15, false);

insert into product_options (product_id, name, extra_price, sort)
select id, '3kg', 0, 0 from products where slug = 'kimchi-pogi';
insert into product_options (product_id, name, extra_price, sort)
select id, '5kg', 20000, 1 from products where slug = 'kimchi-pogi';

insert into articles (slug, type, title, summary, body, image_url, sort) values
('kimchi-story', 'STORY', '기다림이 맛을
만듭니다', '좋은 배추와 천일염, 그리고 옹기 속 시간. 두툼김치는 서두르지 않습니다.',
'[샘플] 김치 브랜드 이야기를 관리자 화면에서 작성해 주세요.', '/images/kimchi-story.jpg', 10);
