-- [샘플 데이터] local/test 프로필 전용. 화면 확인용 예시이며 실제 판매·고객 정보가 아니다.
-- 샘플 회원 비밀번호: sample1234!

-- 1) 기존 상품 상세 채우기
update products set
 description = '두툼장어 시그니처는 살이 두툼하게 오른 장어만 골라 손질한 대표 상품입니다.
초벌 후 급속 냉동해 집에서도 데우기만 하면 촉촉한 식감과 깊은 풍미를 즐길 수 있습니다.',
 weight = '1kg (손질 후 700g 내외)', origin = '국내산 [샘플]',
 features = '· 두께와 윤기를 기준으로 한 마리씩 선별
· 초벌구이 후 급속 냉동으로 풍미 유지
· 전용 데리야끼 소스 동봉',
 process = '선별 → 손질(뼈·내장 제거) → 세척 → 초벌구이 → 급속 냉동 → 진공 포장',
 cooking = '냉장 해동 후 프라이팬 약불에서 껍질 면 3분, 살 면 2분 굽고 소스를 발라 마무리합니다.
에어프라이어는 180℃에서 6~8분.',
 packaging = '진공 포장 + 아이스팩 + 보냉 박스로 발송합니다.',
 faq = 'Q: 해동은 어떻게 하나요?
A: 조리 전날 냉장실로 옮겨 천천히 해동하면 가장 촉촉합니다.
Q: 소스는 몇 개 들어 있나요?
A: 기본 옵션에 소스 2개가 포함되어 있습니다.'
where slug = 'signature';

update products set
 description = '고급 버건디 상자에 시그니처 장어와 전용 소스를 담은 선물세트입니다.
메시지 카드를 함께 보내 마음을 전할 수 있습니다.',
 weight = '시그니처 장어 1kg × 2, 소스 4개', origin = '국내산 [샘플]',
 features = '· 골드 리본을 두른 프리미엄 패키지
· 손글씨 느낌의 메시지 카드 동봉
· 받는 분 주소로 직접 배송',
 packaging = '보냉 포장 후 선물 상자에 담아 쇼핑백과 함께 발송합니다.',
 faq = 'Q: 가격표가 들어가나요?
A: 선물 배송 시 가격표와 거래명세서는 넣지 않습니다.'
where slug = 'royal-gift';

-- 2) 상품 추가
insert into products (slug, name, category, summary, description, price, weight, origin, storage, shipping_info,
  image_url, features, cooking, packaging, stock, featured) values
('signature-500', '두툼장어 실속 500g', 'EEL',
 '혼자 또는 둘이서 즐기기 좋은 실속 구성.',
 '시그니처와 같은 기준으로 선별한 장어를 500g 소포장으로 담았습니다.',
 49000, '500g (손질 후 350g 내외)', '국내산 [샘플]', '냉동 보관 (-18℃ 이하)',
 '결제 완료 후 영업일 기준 1~2일 내 출고',
 '/images/hero-1.jpg', '· 1~2인 소포장
· 소스 1개 동봉', '냉장 해동 후 프라이팬 약불에서 데워 드세요.', '진공 포장 + 보냉 박스', 80, false),
('family-2kg', '두툼장어 패밀리팩 2kg', 'EEL',
 '온 가족이 넉넉하게 즐기는 대용량 구성.',
 '1kg 진공 포장 2팩으로 나누어 보관과 조리가 편리합니다.',
 168000, '2kg (1kg × 2팩)', '국내산 [샘플]', '냉동 보관 (-18℃ 이하)',
 '결제 완료 후 영업일 기준 1~2일 내 출고',
 '/images/dining.jpg', '· 1kg × 2팩 분할 포장
· 소스 4개 동봉', '냉장 해동 후 프라이팬 또는 에어프라이어로 조리하세요.', '진공 포장 + 보냉 박스', 0, false),
('lacquer-gift', '두툼장어 찬합 선물세트', 'GIFT',
 '칠기 찬합에 담아 격식을 더한 선물세트.',
 '전통 칠기 찬합에 장어와 곁들임 소스를 담아 명절·기념일 선물로 좋습니다.',
 119000, '시그니처 장어 1kg, 소스 2개', '국내산 [샘플]', '냉동 보관 (-18℃ 이하)',
 '선물 배송 시 가격표 미동봉',
 '/images/hero-2.jpg', '· 칠기 찬합 패키지
· 메시지 카드 동봉', null, '보냉 포장 후 찬합 상자에 담아 발송', 30, false);

insert into product_options (product_id, name, extra_price, sort)
select id, '기본 (소스 1개)', 0, 0 from products where slug = 'signature-500'
union all select id, '소스 추가', 3000, 1 from products where slug = 'signature-500'
union all select id, '기본 (소스 4개)', 0, 0 from products where slug = 'family-2kg'
union all select id, '기본 포장', 0, 0 from products where slug = 'lacquer-gift'
union all select id, '보자기 포장', 5000, 1 from products where slug = 'lacquer-gift'
union all select id, '기본 포장', 0, 0 from products where slug = 'royal-gift'
union all select id, '보자기 포장', 5000, 1 from products where slug = 'royal-gift';

-- 3) 샘플 회원
insert into members (email, password, name, phone) values
('kim@sample.local',  '{bcrypt}$2a$10$JJ3Fbp9ZeQo/z.zfG2p7G.8mZGt0.JY71WjJxsyun4Fxz62zqXfui', '김서연', '010-1111-2222'),
('lee@sample.local',  '{bcrypt}$2a$10$JJ3Fbp9ZeQo/z.zfG2p7G.8mZGt0.JY71WjJxsyun4Fxz62zqXfui', '이준호', '010-2222-3333'),
('park@sample.local', '{bcrypt}$2a$10$JJ3Fbp9ZeQo/z.zfG2p7G.8mZGt0.JY71WjJxsyun4Fxz62zqXfui', '박지민', '010-3333-4444'),
('choi@sample.local', '{bcrypt}$2a$10$JJ3Fbp9ZeQo/z.zfG2p7G.8mZGt0.JY71WjJxsyun4Fxz62zqXfui', '최민수', '010-4444-5555');

-- 4) 후기
insert into reviews (product_id, member_id, author_name, rating, content, created_at)
select p.id, m.id, v.author, v.rating, v.content, current_timestamp - v.ago
from (values
 ('signature',     'kim@sample.local',  '김*연', 5, '살이 정말 두툼하고 촉촉해요. 소스도 달지 않아서 부모님이 특히 좋아하셨습니다.', interval '9' day),
 ('signature',     'lee@sample.local',  '이*호', 5, '에어프라이어로 데웠는데 겉은 바삭, 속은 부드러웠어요. 재구매 의사 있습니다.', interval '6' day),
 ('signature',     'park@sample.local', '박*민', 4, '맛은 최고인데 양이 조금 아쉬워서 다음엔 패밀리팩으로 주문하려고요.', interval '3' day),
 ('royal-gift',    'choi@sample.local', '최*수', 5, '장인어른 생신 선물로 보냈는데 포장이 고급스러워서 칭찬 많이 들었습니다.', interval '5' day),
 ('lacquer-gift',  'kim@sample.local',  '김*연', 5, '찬합이 예뻐서 다 먹고도 그릇으로 쓰고 있어요.', interval '2' day),
 ('signature-500', 'choi@sample.local', '최*수', 4, '혼자 먹기 딱 좋은 양이에요. 덮밥으로 해 먹었습니다.', interval '1' day)
) as v(slug, email, author, rating, content, ago)
join products p on p.slug = v.slug
join members m on m.email = v.email;

-- 5) 고객 문의
insert into inquiries (member_id, name, email, title, content, answer, created_at, answered_at)
select m.id, v.name, v.email, v.title, v.content, v.answer, current_timestamp - v.ago,
       case when v.answer is null then null else current_timestamp - v.ago + interval '3' hour end
from (values
 ('lee@sample.local', '이준호', 'lee@sample.local', '선물 배송 날짜 지정 가능한가요?', '다음 주 금요일에 도착하도록 보내고 싶습니다.', '네, 주문 시 배송 메모에 희망일을 남겨 주시면 맞춰 출고해 드립니다.', interval '4' day),
 (null, '정하늘', 'sky@sample.local', '대량 주문 문의',                 '회사 명절 선물로 30세트 정도 주문하려고 합니다. 할인이 있을까요?', null, interval '1' day),
 ('park@sample.local', '박지민', 'park@sample.local', '해동 후 재냉동해도 되나요?',     '한 번에 다 못 먹을 것 같아서요.', '품질을 위해 재냉동은 권하지 않습니다. 필요한 만큼만 해동해 주세요.', interval '2' day)
) as v(member_email, name, email, title, content, answer, ago)
left join members m on m.email = v.member_email;

-- 6) 주문 (상태별 1건씩)
insert into orders (order_no, member_id, orderer_name, orderer_phone, orderer_email, recipient_name, recipient_phone,
  zipcode, address1, address2, memo, gift_message, items_amount, shipping_fee, total_amount, status,
  payment_method, paid_at, carrier, tracking_no, created_at, updated_at)
select v.no, m.id, m.name, m.phone, m.email, v.rname, v.rphone, '06234', '서울특별시 강남구 테헤란로 123', v.addr2,
       v.memo, v.gift, v.items, v.fee, v.items + v.fee, v.status, 'TEST',
       case when v.status <> 'PENDING_PAYMENT' then current_timestamp - v.ago end,
       v.carrier, v.tracking, current_timestamp - v.ago, current_timestamp - v.ago
from (values
 ('S260101000001', 'kim@sample.local',  '김서연', '010-1111-2222', '101동 1001호', '부재 시 문 앞', null,                         89000,  4000, 'DELIVERED', 'CJ대한통운', '123456789012', interval '10' day),
 ('S260101000002', 'choi@sample.local', '최영희', '010-9999-8888', '2층',         '경비실 맡겨 주세요', '생신 축하드립니다. 늘 건강하세요.', 159000, 0, 'SHIPPED', 'CJ대한통운', '123456789013', interval '3' day),
 ('S260101000003', 'lee@sample.local',  '이준호', '010-2222-3333', '302호',       null,            null,                         98000,  4000, 'PREPARING', null, null, interval '1' day),
 ('S260101000004', 'park@sample.local', '박지민', '010-3333-4444', '1층',         null,            null,                         49000,  4000, 'PENDING_PAYMENT', null, null, interval '2' hour)
) as v(no, email, rname, rphone, addr2, memo, gift, items, fee, status, carrier, tracking, ago)
join members m on m.email = v.email;

insert into order_items (order_id, product_id, product_name, option_name, unit_price, quantity)
select o.id, p.id, p.name, v.opt, v.price, v.qty
from (values
 ('S260101000001', 'signature',     '기본 (소스 포함) [샘플]', 89000, 1),
 ('S260101000002', 'royal-gift',    '기본 포장',               159000, 1),
 ('S260101000003', 'signature-500', '기본 (소스 1개)',          49000, 2),
 ('S260101000004', 'signature-500', '기본 (소스 1개)',          49000, 1)
) as v(no, slug, opt, price, qty)
join orders o on o.order_no = v.no
join products p on p.slug = v.slug;
