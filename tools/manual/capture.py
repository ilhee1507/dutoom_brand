"""매뉴얼용 화면 캡처: 실제 흐름(주문·가입·후기·문의·관리자 작업)을 Chrome 으로 진행하며
화면(1280x800)과 번호 마커 위치(요소 bounding box)를 shots/markers.json 에 저장한다.
usage: python capture.py <base_url> <out_dir>"""
import json, sys, time
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE, OUT = sys.argv[1].rstrip('/'), Path(sys.argv[2])
OUT.mkdir(parents=True, exist_ok=True)
W, H = 1280, 800
MARKS = {}
NO_ANIM = '*,*::before,*::after{transition:none!important;animation:none!important;caret-color:transparent!important}'


def shot(page, name, marks=(), scroll=None, offset=110):
    """scroll 요소가 화면 위쪽에 오도록 스크롤 후 캡처. marks: [(selector, nth)] 또는 selector"""
    page.add_style_tag(content=NO_ANIM)
    if scroll:
        page.locator(scroll).first.scroll_into_view_if_needed()
        page.evaluate('([s, o]) => { const e = document.querySelector(s); if (e) window.scrollTo(0, e.getBoundingClientRect().top + window.scrollY - o); }', [scroll, offset])
    else:
        page.evaluate('window.scrollTo(0, 0)')
    time.sleep(0.5)
    page.screenshot(path=str(OUT / f'{name}.png'))
    boxes = []
    for m in marks:
        sel, nth = (m, 0) if isinstance(m, str) else m
        loc = page.locator(sel).nth(nth)
        b = loc.bounding_box() if loc.count() else None
        if not b or b['y'] + b['height'] < 0 or b['y'] > H:
            print(f'  ! {name}: marker not visible: {sel}')
            boxes.append(None)
            continue
        boxes.append({k: round(v / (W if k in ('x', 'width') else H), 4) for k, v in b.items()})
    MARKS[name] = boxes
    (OUT / 'markers.json').write_text(json.dumps(MARKS, ensure_ascii=False, indent=1), encoding='utf-8')
    print('shot', name, len(boxes))


def fill(page, values):
    for sel, v in values.items():
        page.fill(sel, v)


with sync_playwright() as p:
    browser = p.chromium.launch(channel='chrome')
    ctx = browser.new_context(viewport={'width': W, 'height': H}, locale='ko-KR')
    page = ctx.new_page()

    # ---------- 사용자: 둘러보기 ----------
    page.goto(BASE + '/')
    shot(page, 'u01_home', ['.site-header .logo', '#gnb', '[data-search-toggle]', '.cart-btn', '.util a[href="/mypage"]'])
    page.click('[data-search-toggle]')
    page.fill('.search-bar input[name=q]', '김치')
    shot(page, 'u02_search', ['.search-bar input[name=q]', '.search-bar button'])
    for code in ('eel', 'kimchi', 'beef'):
        page.goto(BASE + ('/' if code == 'eel' else f'/?brand={code}'))
        shot(page, f'u03_brand_{code}', ['.site-header .logo', '.hero-title'])
    page.goto(BASE + '/?brand=kimchi')
    shot(page, 'u04_home_sections', ['.benefits', '.story'], scroll='.benefits', offset=90)
    page.goto(BASE + '/products?brand=kimchi')
    shot(page, 'u05_list', [('.product-card', 0), ('.product-card .price', 0)], scroll='.product-card', offset=260)

    # ---------- 사용자: 비회원 주문 ----------
    page.goto(BASE + '/products/kimchi-pogi')
    page.select_option('#opt', index=1)
    shot(page, 'u06_detail', ['#opt', '#buy-form .qty', '#buy-form button[value=cart]', '#buy-form button[value=buy]'])
    shot(page, 'u07_detail_tabs', ['.pd-tabs, nav.pd-nav, ul:has(a[href="#reviews"])', '#reviews'], scroll='#reviews', offset=160)
    page.goto(BASE + '/products/kimchi-pogi')
    page.select_option('#opt', index=1)
    page.click('#buy-form button[value=cart]')
    page.wait_for_load_state()
    shot(page, 'u08_cart', ['input[name=quantity]', 'button:has-text("변경")', ':is(a,button):has-text("주문하기")'])
    page.click(':is(a,button):has-text("주문하기")')
    page.wait_for_load_state()
    fill(page, {'#on': '홍길동', '#op': '010-1234-5678', '#oe': 'hong@example.com', '#gp': '1234'})
    shot(page, 'u09_checkout_orderer', ['#on', '#op', '#oe', '#gp'], scroll='#on', offset=180)
    # 카카오 우편번호 (외부 서비스, 팝업)
    try:
        with page.expect_popup(timeout=8000) as pop:
            page.click('[data-postcode]')
        pp = pop.value
        pp.set_viewport_size({'width': 500, 'height': 560})
        pp.wait_for_load_state('networkidle', timeout=15000)
        time.sleep(2)
        pp.screenshot(path=str(OUT / 'u10_postcode.png'))
        MARKS['u10_postcode'] = []
        print('shot u10_postcode (popup)')
        pp.close()
    except Exception as e:
        print('  ! postcode popup:', e)
    fill(page, {'#rn': '김영희', '#rp': '010-9876-5432', '#zc': '06236', '#a1': '서울특별시 강남구 테헤란로 152',
                '#a2': '12층', '#mm': '부재 시 문 앞에 놓아 주세요', '#gm': '늘 건강하세요. 감사합니다.'})
    shot(page, 'u11_checkout_address', ['#rn', '[data-postcode]', '#a2', '#mm', '#gm'], scroll='#rn', offset=150)
    page.check('input[name=agree]')
    shot(page, 'u12_checkout_pay', ['input[name=agree]', 'button:has-text("결제하기")'], scroll='input[name=agree]', offset=420)
    page.click('button:has-text("결제하기")')
    page.wait_for_load_state()
    order_no = page.url.split('/order/')[1].split('/')[0]
    shot(page, 'u13_complete', ['h1', ':is(a,button):has-text("주문 상세 보기")'])

    # 비회원 주문조회 (새 브라우저)
    ctx2 = browser.new_context(viewport={'width': W, 'height': H}, locale='ko-KR')
    g = ctx2.new_page()
    g.goto(BASE + '/order/lookup')
    fill(g, {'#no': order_no, '#pw': '1234'})
    shot(g, 'u14_lookup', ['#no', '#pw', 'button:has-text("조회하기")'])
    g.click('button:has-text("조회하기")')
    g.wait_for_load_state()
    shot(g, 'u15_order_detail', ['h1', 'h2:has-text("배송 조회")'])
    ctx2.close()

    # ---------- 사용자: 회원 ----------
    ctx3 = browser.new_context(viewport={'width': W, 'height': H}, locale='ko-KR')
    m = ctx3.new_page()
    m.goto(BASE + '/signup')
    fill(m, {'#nm': '이매뉴얼', '#em': 'manual@example.com', '#pw': 'manual-pass-1', '#ph': '010-2222-3333'})
    m.check('input[name=agree]')
    shot(m, 'u16_signup', ['#nm', '#em', '#pw', '#ph', 'input[name=agree]', 'button:has-text("가입하기")'], scroll='#nm', offset=200)
    m.click('button:has-text("가입하기")')
    m.wait_for_load_state()
    fill(m, {'input[name=email]': 'manual@example.com', 'input[name=password]': 'manual-pass-1'})
    shot(m, 'u17_login', ['input[name=email]', 'input[name=password]', 'main button[type=submit]', 'main a[href="/order/lookup"]'])
    m.click('main button[type=submit]')
    m.wait_for_load_state()
    # 회원 구매 → 후기 작성 가능
    m.goto(BASE + '/products/kimchi-mat')
    m.click('#buy-form button[value=buy]')
    m.wait_for_load_state()
    fill(m, {'#rn': '이매뉴얼', '#rp': '010-2222-3333', '#zc': '04524', '#a1': '서울특별시 중구 세종대로 110'})
    m.check('input[name=agree]')
    m.click('button:has-text("결제하기")')
    m.wait_for_load_state()
    m.goto(BASE + '/products/kimchi-mat')
    m.select_option('#rating', '5')
    m.fill('#rv', '양념이 깔끔하고 아삭해서 밥반찬으로 딱 좋아요.')
    shot(m, 'u18_review_write', ['#rating', '#rv', 'button:has-text("후기 등록")'], scroll='#reviews', offset=120)
    m.click('button:has-text("후기 등록")')
    m.wait_for_load_state()
    m.goto(BASE + '/inquiry')
    fill(m, {'#tt': '선물 포장 문의', '#ct': '김치 선물세트에 메시지 카드를 넣을 수 있나요?'})
    shot(m, 'u19_inquiry', ['#tt', '#ct', 'button:has-text("문의 남기기")'], scroll='#tt', offset=260)
    m.click('button:has-text("문의 남기기")')
    m.wait_for_load_state()
    m.goto(BASE + '/mypage')
    shot(m, 'u20_mypage', ['h2:has-text("주문 내역")', 'h2:has-text("나의 문의")'], scroll='h1', offset=140)
    m.goto(BASE + '/reviews?brand=kimchi')
    shot(m, 'u21_reviews', [('article.review', 0)], scroll='h1', offset=160)
    ctx3.close()

    # ---------- 관리자 ----------
    ctx4 = browser.new_context(viewport={'width': W, 'height': H}, locale='ko-KR')
    a = ctx4.new_page()
    a.goto(BASE + '/login')
    fill(a, {'input[name=email]': 'admin@dutoom.example', 'input[name=password]': 'admin-password'})
    shot(a, 'a01_login', ['input[name=email]', 'input[name=password]', 'main button[type=submit]'])
    fill(a, {'input[name=email]': 'verify@test.local', 'input[name=password]': 'verify-pass-1'})
    a.click('main button[type=submit]')
    a.wait_for_load_state()
    a.goto(BASE + '/admin')
    shot(a, 'a02_dashboard', ['main nav', 'h2:has-text("출고 대기 주문")', 'h2:has-text("미답변 문의")'])
    a.goto(BASE + '/admin/products')
    shot(a, 'a03_products', ['a:has-text("상품 등록")', 'tbody tr td a'])
    a.click('tbody tr td a:has-text("두툼 포기김치")')
    a.wait_for_load_state()
    shot(a, 'a04_product_basic', ['input[name=name]', 'input[name=slug]', 'select[name=brand]', 'select[name=category]', 'input[name=price]', 'input[name=stock]'], scroll='input[name=name]', offset=150)
    shot(a, 'a05_product_image', ['input[name=image]', 'textarea[name=optionsText]', 'textarea[name=description]'], scroll='input[name=image]', offset=140)
    shot(a, 'a06_product_save', ['input[name=active]', 'input[name=featured]', 'main button:has-text("저장")'], scroll='input[name=active]', offset=480)

    a.goto(BASE + '/admin/brands')
    shot(a, 'a07_brands', ['a:has-text("브랜드 추가")', 'span.tag', ('tbody tr td a[target=_blank]', 1)])
    a.click('a:has-text("브랜드 추가")')
    a.wait_for_load_state()
    fill(a, {'input[name=code]': 'doenjang', 'input[name=sort]': '30', 'input[name=name]': '두툼된장',
             'input[name=productTitle]': '전통 된장', 'input[name=productEyebrow]': 'TRADITIONAL DOENJANG',
             'input[name=primaryColor]': '#5b3a1e', 'input[name=accentColor]': '#c9a050',
             'input[name=eyebrow]': 'AGED IN ONGGI', 'input[name=heroTitle]': '항아리에서 익힌,', 'input[name=heroAccent]': '깊은 된장의 맛',
             'textarea[name=heroSub]': '햇콩과 천일염, 그리고 세월이\n빚어낸 전통 된장.',
             'textarea[name=heroImages]': '/images/kimchi-story.jpg | 장독대에 놓인 옹기',
             'textarea[name=benefits]': 'leaf | 국산 콩 100% | 원재료부터 정직하게\nbox | 옹기 숙성 | 1년 이상 자연 숙성\ntruck | 신선 배송 | 주문 후 바로 출고\ngift | 선물 포장 | 마음을 담은 선물',
             'input[name=signatureImage]': '/images/kimchi-story.jpg | 옹기 항아리',
             'textarea[name=signatureTitle]': '오래 익힐수록\n깊어지는 맛'})
    shot(a, 'a08_brand_basic', ['input[name=code]', 'input[name=sort]', 'input[name=name]', 'input[name=logoFile]', 'input[name=productTitle]', 'input[name=primaryColor]'], scroll='input[name=code]', offset=170)
    shot(a, 'a09_brand_hero', ['input[name=heroTitle]', 'textarea[name=heroSub]', 'textarea[name=heroImages]', 'input[name=heroFile]'], scroll='input[name=eyebrow]', offset=150)
    shot(a, 'a09b_brand_benefits', ['textarea[name=benefits]'], scroll='textarea[name=benefits]', offset=260)
    shot(a, 'a10_brand_story', ['input[name=storySlug]', 'input[name=storyFile]', 'input[name=signatureFile]', 'input[name=giftFile]'], scroll='input[name=storySlug]', offset=150)
    a.check('input[name=productGrid]')
    shot(a, 'a10b_brand_save', ['input[name=productGrid]', 'input[name=guides]', 'main button:has-text("저장")'], scroll='input[name=productGrid]', offset=520)
    a.click('main button:has-text("저장")')
    a.wait_for_load_state()
    shot(a, 'a11_brand_saved', ['.flash', 'a:has-text("브랜드 메인 보기")'])
    a.goto(BASE + '/?brand=doenjang')
    shot(a, 'a12_brand_result', ['.logo-text', '.hero-title', '#gnb'])

    a.goto(BASE + '/admin/orders')
    shot(a, 'a13_orders', ['main p a:has-text("전체")', ('tbody tr td a', 0)])
    a.click(f'tbody a:has-text("{order_no}")')
    a.wait_for_load_state()
    a.select_option('select[name=status]', label='배송중')
    a.fill('input[name=carrier]', 'CJ대한통운')
    a.fill('input[name=trackingNo]', '123456789012')
    shot(a, 'a14_order_detail', ['select[name=status]', 'input[name=carrier]', 'input[name=trackingNo]', 'main button:has-text("저장")'])
    a.click('main button:has-text("저장")')
    a.wait_for_load_state()

    a.goto(BASE + '/admin/articles')
    shot(a, 'a15_articles', ['a:has-text("브랜드 스토리 추가")', 'tbody a:has-text("기다림이")'])
    a.click('tbody a:has-text("기다림이")')
    a.wait_for_load_state()
    shot(a, 'a16_article_form', ['input[name=slug]', 'textarea[name=title]', 'textarea[name=summary], input[name=summary]', 'input[name=image]', 'textarea[name=body]'], scroll='main form', offset=150)

    a.goto(BASE + '/admin/inquiries')
    a.locator('textarea[name=answer]').first.fill('네, 주문서의 "선물 메시지 카드" 칸에 입력해 주시면 카드에 담아 보내 드립니다.')
    shot(a, 'a17_inquiries', [('h2', 0), ('textarea[name=answer]', 0), ('button:has-text("답변")', 0)], scroll='main h2', offset=200)
    a.locator('button:has-text("답변 등록")').first.click()
    a.wait_for_load_state()
    a.goto(BASE + '/admin/reviews')
    shot(a, 'a18_reviews', [('tbody tr', 0), ('button:has-text("삭제")', 0)])
    ctx4.close()
    browser.close()

(OUT / 'markers.json').write_text(json.dumps(MARKS, ensure_ascii=False, indent=1), encoding='utf-8')
print('order', order_no)
