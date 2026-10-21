-- [샘플] 김치 선물세트: 사진(배추김치·깍두기·갓김치 유리병 3종)에 맞춘 구성 + 구매 후기 5건 (local/test 전용)
update products set
  summary = '배추김치·깍두기·갓김치를 유리병에 담아 보자기로 감싼 전통 수제 김치 선물세트.',
  weight = '배추김치 1kg + 깍두기 1kg + 갓김치 1kg [샘플]'
where slug = 'kimchi-gift';

insert into members (email, password, name, phone) values
('han@sample.local', '{bcrypt}$2a$10$JJ3Fbp9ZeQo/z.zfG2p7G.8mZGt0.JY71WjJxsyun4Fxz62zqXfui', '한지우', '010-5555-6666');

insert into reviews (product_id, member_id, author_name, rating, content, created_at)
select p.id, m.id, v.author, v.rating, v.content, current_timestamp - v.ago
from (values
 ('kim@sample.local',  '김*연', 5, '시어머니 생신 선물로 보냈어요. 보자기 포장까지 되어 있어서 따로 포장할 필요가 없었습니다.', interval '12' day),
 ('lee@sample.local',  '이*호', 5, '세 가지 김치가 맛이 다 달라서 좋아요. 특히 갓김치가 쌉싸름하니 별미입니다.', interval '9' day),
 ('park@sample.local', '박*민', 4, '깍두기가 아삭하고 시원해요. 병이 묵직해서 배송 중에 깨질까 걱정했는데 완충재가 꼼꼼했습니다.', interval '6' day),
 ('choi@sample.local', '최*수', 5, '거래처 명절 선물로 10세트 주문했는데 받으신 분들이 다들 맛있다고 연락 주셨어요.', interval '4' day),
 ('han@sample.local',  '한*우', 5, '배추김치 양념이 과하지 않고 깔끔합니다. 유리병이라 냉장고에 그대로 두고 먹기 편해요.', interval '1' day)
) as v(email, author, rating, content, ago)
join products p on p.slug = 'kimchi-gift'
join members m on m.email = v.email;
