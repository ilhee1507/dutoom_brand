-- [샘플] 멀티 브랜드: 미국산 수입소고기 스테이크·도시락 (local/test 전용). /?brand=beef
insert into products (slug, name, brand, category, summary, description, price, weight, origin, storage, shipping_info,
  image_url, stock, featured) values
('beef-ribeye', '미국산 꽃등심 스테이크', 'beef', 'EEL',
 '마블링이 고른 미국산 꽃등심을 3cm 두께로 두툼하게 잘랐습니다.',
 '[샘플] 실제 상품 설명(등급·부위 정보)으로 교체하세요.',
 39000, '300g [샘플]', '소고기: 미국산', '냉장 보관 (0~4℃) [샘플]',
 '[샘플] 주문 후 1~2일 내 출고', '/images/beef-ribeye.jpg', 50, true),
('beef-topblade', '미국산 부채살 스테이크', 'beef', 'EEL',
 '부드러운 식감과 진한 풍미의 부채살 스테이크.',
 '[샘플] 실제 상품 설명으로 교체하세요.',
 26000, '300g [샘플]', '소고기: 미국산', '냉장 보관 (0~4℃) [샘플]',
 '[샘플] 주문 후 1~2일 내 출고', '/images/beef-topblade.jpg', 60, false),
('beef-steak-box', '스테이크 도시락', 'beef', 'EEL',
 '구운 스테이크와 갈릭라이스, 구운 채소를 한 상자에 담은 도시락.',
 '[샘플] 실제 구성으로 교체하세요.',
 12900, '1인분 [샘플]', '소고기: 미국산', '냉동 보관 (-18℃ 이하) [샘플]',
 '[샘플] 전자레인지 3분 · 주문 후 1~2일 내 출고', '/images/beef-steak-box.jpg', 100, false),
('beef-bulgogi-box', '소불고기 도시락', 'beef', 'EEL',
 '달콤 짭조름한 소불고기를 듬뿍 올린 든든한 도시락.',
 '[샘플] 실제 구성으로 교체하세요.',
 9900, '1인분 [샘플]', '소고기: 미국산', '냉동 보관 (-18℃ 이하) [샘플]',
 '[샘플] 전자레인지 3분 · 주문 후 1~2일 내 출고', '/images/beef-bulgogi-box.jpg', 100, false),
('beef-gift', '두툼 스테이크 선물세트', 'beef', 'GIFT',
 '꽃등심·부채살 스테이크를 나무 상자에 담은 선물세트.',
 '[샘플] 실제 선물세트 구성으로 교체하세요.',
 99000, '꽃등심 600g + 부채살 600g [샘플]', '소고기: 미국산', '냉장 보관 (0~4℃) [샘플]',
 '[샘플] 선물 배송 안내', '/images/beef-gift.jpg', 20, false);

insert into product_options (product_id, name, extra_price, sort)
select id, '300g', 0, 0 from products where slug = 'beef-ribeye';
insert into product_options (product_id, name, extra_price, sort)
select id, '600g', 36000, 1 from products where slug = 'beef-ribeye';

insert into articles (slug, type, title, summary, body, image_url, sort) values
('beef-story', 'STORY', '두께가 맛을
만듭니다', '좋은 소고기를 두툼하게, 그리고 제대로 굽는 것. 두툼스테이크가 지키는 단 하나의 원칙입니다.',
'[샘플] 스테이크 브랜드 이야기를 관리자 화면에서 작성해 주세요.', '/images/beef-story.jpg', 11);
