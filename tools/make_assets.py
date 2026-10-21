"""docs/reference-homepage.png(1024x1536 시안)에서 섹션 이미지를 잘라 static/images 로 저장.
- 시안 속 임시 브랜드명(KURAGI)은 덮고 '두툼장어'로 다시 씀
- 히어로에 구워진 슬라이더 버튼은 주변 픽셀로 메움(실제 버튼은 HTML로 렌더)
- img01.jpg는 타 브랜드 표식(좌측 하단, 뚜껑) 영역을 잘라낸 뒤 사용
- story-nature / signature / gift 는 Real-ESRGAN으로 4배 업스케일
  (https://github.com/xinntao/Real-ESRGAN/releases 의 realesrgan-ncnn-vulkan)
실행: ESRGAN=<exe 경로> python tools/make_assets.py  (프로젝트 루트에서)"""
from PIL import Image, ImageChops, ImageDraw, ImageFont, ImageFilter, ImageMath, ImageOps, ImageStat
import os, subprocess, tempfile

SRC = Image.open('docs/reference-homepage.png').convert('RGB')
OUT = 'src/main/resources/static/images'
os.makedirs(OUT, exist_ok=True)
FONT = 'C:/Windows/Fonts/batang.ttc'
GOLD = (198, 164, 106)


def fill_hole(im, box, iters=300):
    """box 내부를 경계 픽셀로부터 확산시켜 메움 (간이 inpaint)."""
    x0, y0, x1, y1 = box
    px = im.load()
    for _ in range(iters):
        for y in range(y0, y1):
            for x in range(x0, x1):
                n = [px[x - 1, y], px[x + 1, y], px[x, y - 1], px[x, y + 1]]
                px[x, y] = tuple(sum(c[i] for c in n) // 4 for i in range(3))
    blur = im.crop((x0 - 4, y0 - 4, x1 + 4, y1 + 4)).filter(ImageFilter.GaussianBlur(2))
    im.paste(blur, (x0 - 4, y0 - 4))


def upscale(im, s=4):
    """Real-ESRGAN(ncnn-vulkan) 4배 업스케일. ESRGAN 환경변수 = realesrgan-ncnn-vulkan.exe 경로."""
    exe = os.environ.get('ESRGAN', 'realesrgan-ncnn-vulkan')
    with tempfile.TemporaryDirectory() as t:
        im.save(f'{t}/i.png')
        subprocess.run([exe, '-i', f'{t}/i.png', '-o', f'{t}/o.png', '-n', 'realesrgan-x4plus', '-s', str(s)],
                       check=True, capture_output=True)
        return Image.open(f'{t}/o.png').convert('RGB')


def save(im, name, q=90):
    im.save(f'{OUT}/{name}', 'JPEG', quality=q, optimize=True, progressive=True)
    print(name, im.size)


# HERO: 텍스트 영역(좌측)을 뺀 장어 사진 + 구워진 슬라이더 버튼 제거
hero = SRC.crop((360, 64, 1024, 420)).copy()
for cx in (902, 945):
    fill_hole(hero, (cx - 360 - 19, 378 - 64 - 19, cx - 360 + 19, 378 - 64 + 19))
save(hero, 'hero-1.jpg')

# img01: 좌측 로고(x<240), 뚜껑 로고(x>1480) 제외
save(Image.open('docs/img01.jpg').convert('RGB').crop((250, 0, 1480, 800)), 'hero-2.jpg')

# BRAND STORY
save(SRC.crop((0, 502, 505, 787)), 'story-craft.jpg')
# story-nature: 시안의 세로 문구를 지운 풍경만 저장(문구는 HTML 세로쓰기로 얹어 칸 비율과 무관하게 안 잘림)
nat = SRC.crop((840, 502, 1024, 787)).copy()
px = nat.load()
# 하늘 구간의 글자: 행마다 좌우 경계 픽셀을 선형 보간 + 미세 노이즈
for x0, y0, x1, y1 in ((104, 30, 153, 124), (128, 124, 153, 140)):
    for y in range(y0, y1):
        a, b = px[x0 - 1, y], px[x1, y]
        for x in range(x0, x1):
            t = (x - x0 + 1) / (x1 - x0 + 1)
            px[x, y] = tuple(round(p + (q - p) * t) for p, q in zip(a, b))
    patch = nat.crop((x0, y0, x1, y1))
    nat.paste(ImageChops.add(patch.filter(ImageFilter.GaussianBlur(0.8)),
                             Image.effect_noise(patch.size, 2).convert('RGB'), 1, -128), (x0, y0))
# 능선을 가로지르는 구간: 측정한 능선 (103,133)→(131,153) 기준으로
# 위쪽은 좌우 하늘 보간, 아래쪽은 왼쪽 12열(왕복)에서 능선으로부터 같은 깊이의 픽셀을 복사 → 능선·산결이 이어짐
ridge = lambda x: 133 + 0.714 * (x - 103)
for x in range(104, 131):
    k = (x - 104) % 22
    sx = 103 - (k if k < 12 else 22 - k)
    t = (x - 103) / 28
    for y in range(110, 186):
        if y < ridge(x):
            a = px[103, y] if y < ridge(103) - 1 else px[131, y]
            px[x, y] = tuple(round(p + (q - p) * t) for p, q in zip(a, px[131, y]))
        else:
            px[x, y] = px[sx, min(284, round(ridge(sx) + y - ridge(x)))]
for x in range(126, 131):  # 오른쪽 원본과 이음매 블렌딩
    t = (x - 125) / 6
    for y in range(110, 186):
        px[x, y] = tuple(round(p + (q - p) * t) for p, q in zip(px[x, y], px[131, y]))
save(upscale(nat), 'story-nature.jpg', 88)

# SIGNATURE 접시
sig = SRC.crop((292, 798, 760, 1046)).copy()
sig.paste(sig.crop((0, 176, 46, 200)), (0, 147))  # 시안 본문 글자 끝부분('니다.')을 아래 배경으로 덮음
# AI 업스케일만 쓰면 그림처럼 매끈해짐 → 2배로 줄여 원본 확대본과 섞고, 샤프닝·그레인으로 사진 질감 복원
W, H = sig.width * 2, sig.height * 2
sig2 = Image.blend(upscale(sig).resize((W, H), Image.LANCZOS), sig.resize((W, H), Image.LANCZOS), 0.45)
sig2 = sig2.filter(ImageFilter.UnsharpMask(1.2, 60, 2))
sig2 = ImageChops.add(sig2, Image.effect_noise((W, H), 5).convert('RGB'), 1, -128)
save(sig2, 'signature.jpg', 90)

# LOGO(임시): docs/logo.png(흰 배경, 검정·빨강) → 4배 업스케일 → 잉크 농도를 알파로 쓰는 골드 단색 투명 PNG
lg = upscale(Image.open('docs/logo.png').convert('RGB'))
ink = lg.convert('RGB').point(lambda v: 255 - v).split()
alpha = ImageChops.lighter(ImageChops.lighter(ink[0], ink[1]), ink[2])  # 255 - min(R,G,B): 검정·빨강=잉크, 흰색=투명
alpha = alpha.point(lambda v: 0 if v < 24 else min(255, int((v - 24) * 1.15)))
# 금박: 대각선 방향 금속 반사 띠(어두운 금↔밝은 금 반복) + 좌상단 광원 엠보스 + 하이라이트
STOPS = [(0, (122, 88, 38)), (.18, (236, 208, 140)), (.30, (255, 243, 200)), (.42, (201, 160, 84)),
         (.58, (128, 92, 40)), (.74, (226, 194, 122)), (.86, (247, 228, 170)), (1, (150, 110, 50))]


def stop_lut(i):
    t = i / 255
    for (t0, c0), (t1, c1) in zip(STOPS, STOPS[1:]):
        if t <= t1:
            s = (t - t0) / (t1 - t0)
            s = s * s * (3 - 2 * s)  # smoothstep: 띠 경계에 선이 생기지 않게
            return [round(a + (b - a) * s) for a, b in zip(c0, c1)]


lut = [stop_lut(i) for i in range(256)]
def diag_grad(size, angle):
    """회전 시 생기는 빈 모서리 경계선이 안 보이도록 큰 정사각 그라데이션을 회전 후 가운데만 씀."""
    n = round((size[0] ** 2 + size[1] ** 2) ** .5) + 2
    g = Image.linear_gradient('L').resize((n, n)).rotate(angle, resample=Image.BICUBIC)
    return g.crop(((n - size[0]) // 2, (n - size[1]) // 2, (n + size[0]) // 2, (n + size[1]) // 2)).resize(size)


diag = diag_grad(lg.size, -24)
gold = Image.merge('RGB', [diag.point([c[ch] for c in lut]) for ch in range(3)])
# 얇은 가장자리 베벨만(과하면 플라스틱처럼 보임). 붓 획 속 미세 틈은 닫고 계산해 금 간 선이 안 생기게
e = alpha.filter(ImageFilter.MaxFilter(7)).filter(ImageFilter.MinFilter(7)).filter(ImageFilter.GaussianBlur(2.5))
bev = ImageChops.subtract(ImageChops.offset(e, 2, 2), ImageChops.offset(e, -2, -2), 1, 128)  # 좌상단 가장자리 > 128
hi = bev.point(lambda v: min(255, max(0, (v - 128) * 2)))
lo = bev.point(lambda v: 255 - min(90, max(0, (128 - v))))
gold = ImageChops.screen(gold, Image.merge('RGB', [hi, hi.point(lambda v: v * 92 // 100), hi.point(lambda v: v * 70 // 100)]))
gold = ImageChops.multiply(gold, Image.merge('RGB', [lo] * 3))
leaf = Image.effect_noise(lg.size, 9).filter(ImageFilter.GaussianBlur(0.7))  # 금박 미세 결
gold = ImageChops.add(gold, Image.merge('RGB', [leaf] * 3), 1, -128)
gold.putalpha(alpha)
gold = gold.crop(alpha.getbbox())
gold.thumbnail((640, 640), Image.LANCZOS)
gold.save(f'{OUT}/logo.png', optimize=True)
print('logo.png', gold.size)

B = ImageFilter.GaussianBlur


def retexture(im, box, patch, feather=28):
    """box의 뭉개진 면: 저주파(조명)는 주변 밝기에 맞추고, 고주파(가죽결)는 patch를 거울 타일링해 입힘."""
    x0, y0, x1, y1 = box[0] - feather, box[1] - feather, box[2] + feather, box[3] + feather
    w, h = x1 - x0, y1 - y0
    p = im.crop(patch)
    tex = Image.new('RGB', (w, h))
    for j, y in enumerate(range(0, h, p.height)):
        for i, x in enumerate(range(0, w, p.width)):
            t = p.transpose(Image.FLIP_LEFT_RIGHT) if i % 2 else p
            tex.paste(t.transpose(Image.FLIP_TOP_BOTTOM) if j % 2 else t, (x, y))
    tgt = im.crop((x0, y0, x1, y1))
    hp = ImageChops.subtract(tex, tex.filter(B(8)), 1, 128)
    low = tgt.filter(B(8))
    m = Image.new('L', (w, h))
    m.paste(255, (feather, feather, w - feather, h - feather))
    # 지운 자리는 원래 금색 글자 탓에 밝음 → 안쪽 밝기를 바깥 링 평균에 맞춤
    ring, inner = ImageStat.Stat(tgt, ImageOps.invert(m)).mean, ImageStat.Stat(tgt, m).mean
    adj = Image.merge('RGB', [c.point(lambda v, r=r / i: min(255, int(v * r))) for c, r, i in zip(low.split(), ring, inner)])
    low = Image.composite(adj, low, m.filter(B(feather / 2)))
    im.paste(Image.composite(ImageChops.add(low, hp, 1, -128), tgt, m.filter(B(feather / 2))), (x0, y0))


def stamp(im, cx, cy, w, sx, sy, depth=1.0):
    """logo 알파를 음각 금박으로 찍음. sx: 면의 가로축 기울기(dy/dx), sy: 세로축 기울기(dx/dy)."""
    a = gold.getchannel('A')
    a = a.resize((w, round(a.height * w / a.width)), Image.LANCZOS)
    pad = int(w * 0.3)
    cw, ch = a.width + 2 * pad, a.height + 2 * pad
    flat = Image.new('L', (cw, ch))
    flat.paste(a, (pad, pad))
    det, c0x, c0y = 1 - sx * sy, cw / 2, ch / 2
    m = flat.transform((cw, ch), Image.AFFINE,
                       (1 / det, -sy / det, c0x - c0x / det + sy * c0y / det,
                        -sx / det, 1 / det, c0y + sx * c0x / det - c0y / det), Image.BICUBIC).filter(B(0.6))
    box = (round(cx - cw / 2), round(cy - ch / 2))
    bg = im.crop((box[0], box[1], box[0] + cw, box[1] + ch))
    lum = bg.convert('L')
    grain = ImageChops.subtract(lum, lum.filter(B(2.5)), 1, 128)  # 바탕 결이 박 표면에 비침
    e, k = m.filter(B(2.2)), max(1, round(w / 110))  # 음각 베벨: 빛은 좌상단 → 글자 안쪽 좌상단 벽이 어두움
    bev = ImageChops.subtract(ImageChops.offset(e, k, k), ImageChops.offset(e, -k, -k), 1, 128)
    sheen = diag_grad((cw, ch), -28)
    sheen = sheen.point(lambda v: int(128 + 95 * max(-1.0, 1 - abs(v / 255 - 0.42) * 4.2)))  # 대각선 금속 반사
    shade = ImageMath.lambda_eval(
        lambda d: 112 + (d['float'](d['s']) - 128) * 0.55 + (d['float'](d['b']) - 128) * 1.6 * depth
        + (d['float'](d['g']) - 128) * 1.3, s=sheen, b=bev, g=grain).convert('L')
    foil = ImageOps.colorize(shade, black=(62, 36, 10), mid=(196, 150, 70), white=(255, 238, 178), midpoint=112).filter(B(0.5))
    halo = m.filter(B(k * 3)).point(lambda v: 255 - int(v * 0.32 * depth))  # 눌린 자리 주변이 살짝 어두워짐
    bg = ImageChops.multiply(bg, Image.merge('RGB', [halo] * 3))
    spk = Image.effect_noise((cw, ch), 60).filter(B(0.8)).point(lambda v: 255 if v > 70 else 150 + v)  # 박 미세 빠짐
    out = Image.composite(foil, bg, ImageChops.multiply(m, spk).point(lambda v: int(v * 0.96)))
    im.paste(ImageChops.add(out, Image.effect_noise((cw, ch), 4).convert('RGB'), 1, -128), box)


# GIFT: 박스의 KURAGI 로고·카드 문구를 덮고(1배) → 업스케일 → 가죽결 복원 → logo.png를 음각 금박으로 찍음
gift = SRC.crop((372, 1048, 830, 1300)).copy()
ox, oy = 372, 1048
fill_hole(gift, (552 - ox, 1124 - oy, 644 - ox, 1178 - oy), 600)  # 박스 로고
fill_hole(gift, (457 - ox, 1228 - oy, 489 - ox, 1252 - oy), 300)  # 카드 문구(금색 테두리 안쪽만)
fill_hole(gift, (464 - ox, 1255 - oy, 477 - ox, 1264 - oy), 200)  # 카드 하단 작은 마크
gift = upscale(gift)
retexture(gift, (720, 304, 1088, 520), (560, 290, 715, 560))
stamp(gift, 904, 418, 300, 0.115, -0.03)  # 상자 면: 테두리 선 기준 기울기
stamp(gift, 404, 766, 112, 0.072, -0.072, depth=0.55)  # 카드: 약 4° 회전, 매끈한 종이라 음각 약하게
save(gift, 'gift.jpg', 88)

# DINING
save(SRC.crop((0, 1301, 352, 1461)), 'dining.jpg')
# GUIDE 카드: 시안 썸네일(113px)은 업스케일만으로는 그림 같아서, tools/sd_guides.py(SDXL img2img)로
# 같은 구도의 음식 사진으로 다시 그린 docs/guide-N-sd.jpg(1152x768)를 웹 크기로 줄임
for i in (1, 2, 3):
    save(Image.open(f'docs/guide-{i}-sd.jpg').convert('RGB').resize((720, 480), Image.LANCZOS), f'guide-{i}.jpg', 86)

# 브랜드 스토리 '장인의 손끝에서' 대표 이미지: tools/sd_story.py craft (seed 42) 생성본
save(Image.open('docs/story-grill-sd.jpg').convert('RGB').resize((960, 640), Image.LANCZOS), 'story-grill.jpg', 86)
