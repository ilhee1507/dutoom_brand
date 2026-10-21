-- [샘플] 스테이크 선물세트: 사진(남색 선물상자·진공포장 4팩·금색 리본·메시지 카드) 기준 구성·상세 + 구매 후기 5건 (local/test 전용)
-- 등급(USDA Prime/Choice 등)은 실제 판매 등급 확인 전까지 표기하지 않음
update products set
  summary = '두툼하게 자른 미국산 꽃등심·부채살 스테이크 4팩을 남색 선물상자에 담은 프리미엄 선물세트.',
  weight = '꽃등심 300g × 2 + 부채살 300g × 2 (총 1.2kg) [샘플]',
  description = '[샘플] 마음을 전하는 두툼한 한 상.

미국산 꽃등심과 부채살을 3cm 두께로 두툼하게 잘라 한 팩씩 진공 포장했습니다.
남색 선물상자에 로즈마리와 함께 담고 금색 리본과 메시지 카드를 더해, 받는 분께 그대로 전하실 수 있습니다.

명절 선물, 집들이·생일 선물, 거래처 감사 선물로 추천합니다.',
  features = '[샘플]
· 꽃등심 2팩: 고른 마블링으로 부드럽고 고소한 풍미
· 부채살 2팩: 진한 육향과 쫄깃한 식감
· 3cm 두께 커팅: 겉은 바삭, 속은 촉촉하게 굽기 좋은 두께
· 1팩 1인분(300g) 진공 포장: 필요한 만큼만 꺼내 굽기 편리',
  cooking = '[샘플]
1. 굽기 30분 전 냉장고에서 꺼내 실온에 둡니다.
2. 키친타월로 물기를 닦고 소금·후추로 간합니다.
3. 강불로 달군 팬에 앞뒤 각 2~3분씩 굽고, 버터·마늘·로즈마리를 넣어 끼얹어 줍니다.
4. 불에서 내려 5분간 레스팅한 뒤 썰어 드세요.',
  packaging = '[샘플]
· 진공 포장 4팩을 아이스팩과 함께 보냉 포장 후 선물상자에 담아 출고합니다.
· 금색 리본과 메시지 카드가 기본 포함됩니다.
· 냉장 배송되며, 받으신 후 바로 냉장(0~4℃) 보관해 주세요. 3일 안에 드시지 않을 경우 냉동 보관을 권합니다.'
where slug = 'beef-gift';

insert into reviews (product_id, member_id, author_name, rating, content, created_at)
select p.id, m.id, v.author, v.rating, v.content, current_timestamp - v.ago
from (values
 ('kim@sample.local',  '김*연', 5, '아버님 생신 선물로 보냈어요. 상자를 열자마자 리본이랑 카드까지 있어서 너무 고급스럽다고 하셨습니다.', interval '11' day),
 ('lee@sample.local',  '이*호', 5, '꽃등심이 정말 두툼해서 겉은 바삭하게, 속은 미디엄레어로 딱 맞게 구워졌어요. 집에서 레스토랑 기분 냈습니다.', interval '8' day),
 ('park@sample.local', '박*민', 4, '부채살이 생각보다 훨씬 부드러웠어요. 한 팩씩 진공 포장이라 나눠서 보관하기 좋습니다.', interval '5' day),
 ('choi@sample.local', '최*수', 5, '거래처 명절 선물로 보냈는데 아이스팩까지 꼼꼼하게 들어 있어서 신선하게 잘 받았다고 연락 왔어요.', interval '3' day),
 ('han@sample.local',  '한*우', 5, '상세페이지 굽는 법 그대로 따라 했더니 실패 없이 맛있게 먹었습니다. 재구매 의사 있어요.', interval '1' day)
) as v(email, author, rating, content, ago)
join products p on p.slug = 'beef-gift'
join members m on m.email = v.email;
