// 두툼장어 공통 스크립트 (의존성 없음)
document.addEventListener('DOMContentLoaded', () => {
    // 고정 헤더 그림자
    const header = document.querySelector('.site-header');
    const onScroll = () => header && header.classList.toggle('is-scrolled', window.scrollY > 10);
    window.addEventListener('scroll', onScroll, {passive: true});
    onScroll();

    // 모바일 햄버거 메뉴
    const menuBtn = document.querySelector('[data-menu-toggle]');
    const gnb = document.getElementById('gnb');
    menuBtn?.addEventListener('click', () => {
        const open = gnb.classList.toggle('is-open');
        menuBtn.setAttribute('aria-expanded', open);
        document.body.style.overflow = open ? 'hidden' : '';
    });

    // 검색창 토글
    const searchBar = document.querySelector('.search-bar');
    document.querySelector('[data-search-toggle]')?.addEventListener('click', () => {
        searchBar.hidden = !searchBar.hidden;
        if (!searchBar.hidden) searchBar.querySelector('input').focus();
    });

    // 히어로: 부드러운 Fade 슬라이드만 사용
    const slider = document.querySelector('[data-slider]');
    if (slider) {
        const slides = [...slider.querySelectorAll('.hero-slide')];
        const cur = slider.querySelector('[data-cur]');
        const pauseBtn = slider.querySelector('[data-pause]');
        const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        let i = 0, timer = null;
        const show = n => {
            slides[i].classList.remove('is-active');
            i = (n + slides.length) % slides.length;
            slides[i].classList.add('is-active');
            cur.textContent = String(i + 1).padStart(2, '0');
        };
        const play = () => { timer = setInterval(() => show(i + 1), 6000); };
        const stop = () => { clearInterval(timer); timer = null; };
        slider.querySelector('[data-prev]').addEventListener('click', () => show(i - 1));
        pauseBtn.addEventListener('click', () => {
            if (timer) stop(); else play();
            pauseBtn.querySelector('use').setAttribute('href', timer ? '#i-pause' : '#i-play');
            pauseBtn.setAttribute('aria-label', timer ? '자동 재생 일시정지' : '자동 재생 시작');
        });
        if (!reduce && slides.length > 1) play();
        else pauseBtn.querySelector('use').setAttribute('href', '#i-play');
    }

    // 수량 스테퍼 + 상세페이지 합계
    document.querySelectorAll('[data-buy-form]').forEach(form => {
        const qty = form.querySelector('input[name=quantity]');
        const opt = form.querySelector('select[name=optionId]');
        const total = form.querySelector('[data-total]');
        const base = Number(form.dataset.price || 0);
        const update = () => {
            if (!total) return;
            const extra = opt ? Number(opt.selectedOptions[0]?.dataset.extra || 0) : 0;
            total.textContent = ((base + extra) * Number(qty.value || 1)).toLocaleString('ko-KR') + '원';
        };
        form.querySelectorAll('[data-qty]').forEach(b => b.addEventListener('click', () => {
            qty.value = Math.min(99, Math.max(1, Number(qty.value || 1) + Number(b.dataset.qty)));
            update();
        }));
        qty.addEventListener('change', update);
        opt?.addEventListener('change', update);
        update();
    });
});
