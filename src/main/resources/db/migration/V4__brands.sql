-- 멀티 브랜드: 관리자 화면에서 추가·수정 (기존 application.yml shop.brands 대체). sort 가 가장 작은 브랜드가 기본(/)
-- 이미지 칸은 "경로 | 대체텍스트", hero_images 는 줄마다 "경로 | 대체텍스트", benefits 는 줄마다 "아이콘 | 제목 | 설명"
create table brands (
    code varchar(30) primary key,
    sort integer not null default 0,
    name varchar(60) not null,
    logo varchar(500),
    eyebrow varchar(120),
    hero_title varchar(120),
    hero_accent varchar(120),
    hero_sub varchar(500),
    hero_images text,
    product_title varchar(60) not null,
    product_eyebrow varchar(120),
    story_slug varchar(80),
    story_image varchar(500),
    nature_image varchar(500),
    nature_copy varchar(300),
    signature_title varchar(200),
    signature_text varchar(500),
    signature_image varchar(500),
    gift_image varchar(500),
    benefits text,
    product_grid boolean not null default false,
    guides boolean not null default false,
    primary_color varchar(9),
    accent_color varchar(9)
);

insert into brands (code, sort, name, logo, eyebrow, hero_title, hero_accent, hero_sub, hero_images, product_title, product_eyebrow,
  story_slug, story_image, nature_image, nature_copy, signature_title, signature_text, signature_image, guides) values
('eel', 0, '두툼장어', '/images/logo.png | 두툼장어 DUTUM JANG-EO', 'THE ART OF PREMIUM EEL', '두툼한 한 점,', '깊이 있는 품격',
 '정성스럽게 완성한 깊은 풍미,
특별한 식탁을 위한 프리미엄 장어.',
 '/images/hero-1.jpg | 검은 접시 위에 윤기 나게 구운 두툼한 장어구이
/images/hero-2.jpg | 칠기 찬합에 담긴 장어구이',
 '프리미엄 장어', 'PREMIUM EEL', 'brand-story',
 '/images/story-craft.jpg | 장어를 손질하는 장인의 손', '/images/story-nature.jpg | 안개 낀 산과 강 풍경',
 '자연이 키우고
사람이 완성합니다',
 '오직 하나,
가장 좋은 장어',
 '육즙이 풍부하고 부드러운 식감,
깊은 풍미를 느낄 수 있는 프리미엄 장어입니다.',
 '/images/signature.jpg | 검은 도자기 접시에 담긴 장어구이', true);
