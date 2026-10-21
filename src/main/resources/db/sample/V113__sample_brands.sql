-- [샘플] 김치·스테이크 브랜드 (기존 application.yml 샘플을 brands 테이블로 이전) (local/test 전용)
insert into brands (code, sort, name, eyebrow, hero_title, hero_accent, hero_sub, hero_images, product_title, product_eyebrow,
  story_slug, story_image, signature_title, signature_text, signature_image, gift_image, benefits, product_grid, primary_color, accent_color) values
('kimchi', 10, '두툼김치', 'THE TASTE OF TIME', '기다림으로 익힌,', '깊은 김치의 맛',
 '좋은 배추와 천일염, 옹기 속 시간이
완성한 정직한 김치.',
 '/images/kimchi-hero.jpg | 검은 그릇에 담긴 참깨 뿌린 김치',
 '프리미엄 김치', 'PREMIUM KIMCHI', 'kimchi-story', '/images/kimchi-story.jpg | 아침 안개 속 장독대의 옹기',
 '매일 먹어도
질리지 않는 김치',
 '아삭한 식감과 깊게 밴 양념,
식탁의 기본을 다시 생각합니다.',
 '/images/kimchi-signature.jpg | 잎마다 양념이 고루 밴 통포기김치',
 '/images/kimchi-gift-main.jpg | 배추김치·깍두기·갓김치 유리병을 담은 김치 선물세트와 보자기',
 'leaf | 국산 배추·천일염 | 원재료부터 정직하게
knife | 손으로 버무린 양념 | 전통 방식 그대로
truck | 냉장 신선 배송 | 담근 그대로 전달
gift | 옹기 선물 패키지 | 마음을 담은 선물',
 true, '#7a1e1e', '#d8a24a'),
('beef', 20, '두툼스테이크', 'PRIME CUT STEAK & DOSIRAK', '두툼하게 구운 한 점,', '육즙 가득한 스테이크',
 '미국산 소고기를 두툼하게 잘라
스테이크와 도시락으로 완성했습니다.',
 '/images/beef-hero.jpg | 나무 도마 위에 썰어 놓은 미디엄레어 스테이크',
 '스테이크·도시락', 'STEAK & DOSIRAK', 'beef-story', '/images/beef-story.jpg | 숯불 그릴 위에서 집게로 굽는 두툼한 뼈등심 스테이크',
 '두께가 다르면
맛이 다릅니다',
 '겉은 바삭하게, 속은 촉촉하게.
집에서도 레스토랑의 스테이크를.',
 '/images/beef-signature.jpg | 도마 위 유산지에 놓인 마블링 고른 두툼한 꽃등심',
 '/images/beef-gift-main.jpg | 남색 선물상자에 담긴 마블링 스테이크 4팩과 금색 리본',
 'badge | 미국산 소고기 | 원산지 정직 표기
knife | 두툼한 수제 커팅 | 한 장씩 손질
truck | 냉장·냉동 신선 배송 | 콜드체인 그대로
box | 데우기만 하는 도시락 | 전자레인지 3분',
 true, '#4a2418', '#c9893f');
