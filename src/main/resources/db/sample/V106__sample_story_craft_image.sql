-- [샘플] 장인 스토리 대표 이미지를 대표 브랜드 스토리(story-craft.jpg)와 겹치지 않게 숯불 초벌구이 이미지로 교체
update articles set image_url = '/images/story-grill.jpg' where slug = 'craftsmanship' and image_url = '/images/story-craft.jpg';
