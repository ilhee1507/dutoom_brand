-- [샘플] 김치 상품별 대표 이미지 (V107 은 모두 같은 이미지였음). 포기김치는 실제 포기 형태 사진
update products set image_url = '/images/kimchi-pogi.jpg' where slug = 'kimchi-pogi';
update products set image_url = '/images/kimchi-mat.jpg' where slug = 'kimchi-mat';
update products set image_url = '/images/kimchi-gift.jpg' where slug = 'kimchi-gift';
