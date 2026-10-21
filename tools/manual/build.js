// 두툼 쇼핑몰 사용자/관리자 매뉴얼 PPT 생성. usage: node build.js <shots_dir> <out_dir>
const pptxgen = require('pptxgenjs');
const fs = require('fs');
const path = require('path');
const { applyTheme } = require(process.env.PPTX_SKILL + '/scripts/apply_theme.js');

const SHOTS = process.argv[2];
const OUT = process.argv[3];
const MARKS = JSON.parse(fs.readFileSync(path.join(SHOTS, 'markers.json'), 'utf8'));
const img = (n) => path.join(SHOTS, n + '.png');

const THEME = {
  name: 'Dutoom Manual',
  headFontFace: 'Malgun Gothic',
  bodyFontFace: 'Malgun Gothic',
  colors: {
    dk1: '1F1A17', lt1: 'FFFFFF', dk2: '5A1F24', lt2: 'F3EEE8',
    accent1: 'B8893E', accent2: 'E5484D', accent3: '7A1E1E', accent4: '6B5E55', accent5: 'D9CFC4', accent6: '2E2824',
    hlink: '7A1E1E', folHlink: '6B5E55',
  },
};
const HL = THEME.colors.accent2;   // 화면 위 강조(빨강) — shadow 등 hex 전용 옵션용

// 화면 슬라이드 공통 배치 (LAYOUT_WIDE 13.333 x 7.5)
const IMG = { x: 0.6, y: 1.5, w: 8.0, h: 5.0 };
const PANEL = { x: 8.95, w: 3.8 };

function build(spec) {
  const pres = new pptxgen();
  pres.layout = 'LAYOUT_WIDE';
  pres.theme = { headFontFace: THEME.headFontFace, bodyFontFace: THEME.bodyFontFace };
  pres.title = spec.title;
  pres.author = '두툼 쇼핑몰';
  const C = pres.SchemeColor;

  pres.defineSlideMaster({
    title: 'Title Dark', background: { color: C.text1 },
    objects: [
      { placeholder: { options: { name: 'title', type: 'title', x: 0.7, y: 2.2, w: 6.0, h: 1.9, fontSize: 40, bold: true, color: C.background1, valign: 'bottom', align: 'left', margin: 0 }, text: '' } },
      { placeholder: { options: { name: 'body', type: 'body', x: 0.7, y: 4.3, w: 6.0, h: 1.2, fontSize: 18, color: C.accent5, valign: 'top', margin: 0 }, text: '' } },
    ],
  });
  pres.defineSlideMaster({
    title: 'Section Dark', background: { color: C.text1 },
    objects: [
      { placeholder: { options: { name: 'title', type: 'title', x: 0.7, y: 3.0, w: 6.2, h: 1.0, fontSize: 36, bold: true, color: C.background1, valign: 'top', align: 'left', margin: 0 }, text: '' } },
      { placeholder: { options: { name: 'body', type: 'body', x: 0.7, y: 4.1, w: 6.2, h: 1.4, fontSize: 16, color: C.accent5, valign: 'top', margin: 0 }, text: '' } },
    ],
    slideNumber: { x: 12.3, y: 6.95, w: 0.6, h: 0.3, fontSize: 10, color: C.accent4, align: 'right' },
  });
  pres.defineSlideMaster({
    title: 'Screen', background: { color: C.background1 },
    objects: [
      { placeholder: { options: { name: 'title', type: 'title', x: 0.6, y: 0.35, w: 12.1, h: 0.65, fontSize: 28, bold: true, color: C.text1, valign: 'middle', align: 'left', margin: 0 }, text: '' } },
      { placeholder: { options: { name: 'body', type: 'body', x: 0.6, y: 0.98, w: 12.1, h: 0.4, fontSize: 14, color: C.accent4, valign: 'middle', margin: 0 }, text: '' } },
      { text: { text: spec.footer, options: { x: 0.6, y: 6.95, w: 6, h: 0.3, fontSize: 10, color: C.accent4, margin: 0 } } },
    ],
    slideNumber: { x: 12.3, y: 6.95, w: 0.6, h: 0.3, fontSize: 10, color: C.accent4, align: 'right' },
  });

  let section = null;
  const add = (master) => pres.addSlide({ masterName: master, sectionTitle: section });

  // 번호 원 + 굵은 제목 + 설명 목록 (오른쪽 패널)
  function steps(slide, items, y0, gap) {
    items.forEach((it, i) => {
      const y = y0 + i * gap;
      slide.addShape(pres.shapes.OVAL, { x: PANEL.x, y, w: 0.38, h: 0.38, fill: { color: C.accent2 }, line: { color: C.accent2 } });
      slide.addText(String(i + 1), { x: PANEL.x, y, w: 0.38, h: 0.38, fontSize: 13, bold: true, color: C.background1, align: 'center', valign: 'middle', margin: 0, isTextBox: true });
      slide.addText([
        { text: it[0], options: { fontSize: 15, bold: true, color: C.text1, breakLine: true } },
        { text: it[1], options: { fontSize: 12, color: C.accent4 } },
      ], { x: PANEL.x + 0.5, y: y - 0.04, w: PANEL.w - 0.5, h: gap - 0.08, valign: 'top', margin: 0, paraSpaceAfter: 2, isTextBox: true });
    });
  }

  function tipBox(slide, text, y, h) {
    slide.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: PANEL.x, y, w: PANEL.w, h, fill: { color: C.background2 }, line: { color: C.background2 }, rectRadius: 0.08 });
    slide.addText([{ text: 'TIP  ', options: { bold: true, color: C.accent3 } }, { text, options: { color: C.text1 } }],
      { x: PANEL.x + 0.15, y: y + 0.08, w: PANEL.w - 0.3, h: h - 0.16, fontSize: 12, valign: 'middle', margin: 0, isTextBox: true });
  }

  function picture(slide, name, box) {
    slide.addShape(pres.shapes.RECTANGLE, { ...box, fill: { color: C.background1 }, line: { color: C.accent5, width: 1 },
      shadow: { type: 'outer', color: '000000', opacity: 0.18, blur: 8, offset: 3, angle: 90 } });
    slide.addImage({ path: img(name), ...box });
  }

  // 화면 위 강조 상자 + 번호 (markers.json 의 비율 좌표 → 슬라이드 좌표)
  function marks(slide, name, which, box) {
    const ms = MARKS[name] || [];
    const placed = [];
    which.forEach((mi, i) => {
      const b = ms[mi];
      if (!b) { console.warn('missing marker', name, mi); return; }
      // 화면 밖으로 나가는 부분은 잘라 낸다 (옮기면 다른 칸과 겹침)
      const pad = 0.04;
      const x0 = Math.max(box.x + b.x * box.w - pad, box.x), y0 = Math.max(box.y + b.y * box.h - pad, box.y);
      const x1 = Math.min(box.x + (b.x + b.width) * box.w + pad, box.x + box.w), y1 = Math.min(box.y + (b.y + b.height) * box.h + pad, box.y + box.h);
      const cx = x0, cy = y0, w = Math.max(x1 - x0, 0.2), h = Math.max(y1 - y0, 0.16);
      slide.addShape(pres.shapes.RECTANGLE, { x: cx, y: cy, w, h, fill: { color: C.accent2, transparency: 100 }, line: { color: C.accent2, width: 2.25 } });
      const d = 0.34;
      let bx = Math.min(Math.max(cx - d / 2, box.x - d / 2), box.x + box.w - d / 2);
      const by = Math.min(Math.max(cy - d / 2, box.y - d / 2), box.y + box.h - d / 2);
      const hit = () => placed.some((p) => Math.abs(p[0] - bx) < d && Math.abs(p[1] - by) < d);
      if (hit()) bx = Math.min(cx + w - d / 2, box.x + box.w - d / 2);   // 번호끼리 겹치면 상자 오른쪽 끝으로 (글자 가림 방지)
      while (hit()) bx -= d + 0.04;
      placed.push([bx, by]);
      slide.addShape(pres.shapes.OVAL, { x: bx, y: by, w: d, h: d, fill: { color: C.accent2 }, line: { color: C.background1, width: 1.5 } });
      slide.addText(String(i + 1), { x: bx, y: by, w: d, h: d, fontSize: 12, bold: true, color: C.background1, align: 'center', valign: 'middle', margin: 0, isTextBox: true });
    });
  }

  for (const s of spec.slides) {
    if (s.type === 'title') {
      section = s.section;
      pres.addSection({ title: section });
      const slide = add('Title Dark');
      slide.addText(s.title, { placeholder: 'title' });
      slide.addText(s.sub, { placeholder: 'body' });
      picture(slide, s.img, { x: 7.3, y: 1.6, w: 5.4, h: 3.375 });
      slide.addText(s.note, { x: 0.7, y: 6.5, w: 11.9, h: 0.4, fontSize: 12, color: C.accent5, margin: 0, isTextBox: true });
    } else if (s.type === 'section') {
      section = s.title;
      pres.addSection({ title: section });
      const slide = add('Section Dark');
      slide.addText(s.no, { x: 0.7, y: 1.4, w: 3, h: 1.4, fontSize: 80, bold: true, color: C.accent1, margin: 0, isTextBox: true });
      slide.addText(s.title, { placeholder: 'title' });
      slide.addText(s.desc, { placeholder: 'body' });
      picture(slide, s.img, { x: 7.5, y: 1.9, w: 5.2, h: 3.25 });
    } else if (s.type === 'shot') {
      const slide = add('Screen');
      slide.addText(s.title, { placeholder: 'title' });
      slide.addText(s.sub, { placeholder: 'body' });
      picture(slide, s.img, IMG);
      marks(slide, s.img, s.marks.map((m) => m[0]), IMG);
      const n = s.marks.length;
      const gap = n <= 3 ? 1.05 : n <= 4 ? 0.95 : n <= 5 ? 0.8 : 0.72;
      steps(slide, s.marks.map((m) => [m[1], m[2]]), IMG.y + 0.05, gap);
      if (s.tip) tipBox(slide, s.tip, IMG.y + IMG.h - 1.2, 1.2);
    } else if (s.type === 'popup') {
      const slide = add('Screen');
      slide.addText(s.title, { placeholder: 'title' });
      slide.addText(s.sub, { placeholder: 'body' });
      const box = { x: 0.6, y: 1.5, w: 4.46, h: 5.0 };
      picture(slide, s.img, box);
      const x0 = 5.6;
      s.steps.forEach((st, i) => {
        const y = 1.6 + i * 1.15;
        slide.addShape(pres.shapes.OVAL, { x: x0, y, w: 0.5, h: 0.5, fill: { color: C.accent2 }, line: { color: C.accent2 } });
        slide.addText(String(i + 1), { x: x0, y, w: 0.5, h: 0.5, fontSize: 16, bold: true, color: C.background1, align: 'center', valign: 'middle', margin: 0, isTextBox: true });
        slide.addText([{ text: st[0], options: { fontSize: 18, bold: true, color: C.text1, breakLine: true } }, { text: st[1], options: { fontSize: 14, color: C.accent4 } }],
          { x: x0 + 0.7, y: y - 0.05, w: 6.4, h: 1.0, valign: 'top', margin: 0, isTextBox: true });
      });
      if (s.tip) {
        slide.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: x0, y: 5.5, w: 7.1, h: 1.0, fill: { color: C.background2 }, line: { color: C.background2 }, rectRadius: 0.08 });
        slide.addText([{ text: 'TIP  ', options: { bold: true, color: C.accent3 } }, { text: s.tip, options: { color: C.text1 } }],
          { x: x0 + 0.2, y: 5.55, w: 6.7, h: 0.9, fontSize: 13, valign: 'middle', margin: 0, isTextBox: true });
      }
    } else if (s.type === 'trio') {
      const slide = add('Screen');
      slide.addText(s.title, { placeholder: 'title' });
      slide.addText(s.sub, { placeholder: 'body' });
      s.items.forEach((it, i) => {
        const box = { x: 0.6 + i * 4.1, y: 1.65, w: 3.85, h: 2.406 };
        picture(slide, it[0], box);
        slide.addText([{ text: it[1], options: { fontSize: 18, bold: true, color: C.text1, breakLine: true } },
                       { text: it[2], options: { fontSize: 14, color: C.accent3, breakLine: true } },
                       { text: it[3], options: { fontSize: 12, color: C.accent4 } }],
          { x: box.x, y: 4.25, w: box.w, h: 1.3, valign: 'top', margin: 0, paraSpaceAfter: 4, isTextBox: true });
      });
      if (s.tip) {
        slide.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: 0.6, y: 5.75, w: 12.05, h: 0.8, fill: { color: C.background2 }, line: { color: C.background2 }, rectRadius: 0.08 });
        slide.addText([{ text: 'TIP  ', options: { bold: true, color: C.accent3 } }, { text: s.tip, options: { color: C.text1 } }],
          { x: 0.8, y: 5.8, w: 11.7, h: 0.7, fontSize: 13, valign: 'middle', margin: 0, isTextBox: true });
      }
    } else if (s.type === 'cards') {
      const slide = add('Screen');
      slide.addText(s.title, { placeholder: 'title' });
      slide.addText(s.sub, { placeholder: 'body' });
      const cols = s.cols || 3, cw = (12.05 - (cols - 1) * 0.3) / cols;
      const rows = Math.ceil(s.items.length / cols), ch = Math.min(1.6, (5.2 - (rows - 1) * 0.3) / rows);
      s.items.forEach((it, i) => {
        const x = 0.6 + (i % cols) * (cw + 0.3), y = 1.6 + Math.floor(i / cols) * (ch + 0.3);
        slide.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y, w: cw, h: ch, fill: { color: C.background2 }, line: { color: C.background2 }, rectRadius: 0.1 });
        slide.addShape(pres.shapes.OVAL, { x: x + 0.25, y: y + 0.25, w: 0.5, h: 0.5, fill: { color: s.numbered === false ? C.accent1 : C.accent3 }, line: { color: C.background2 } });
        slide.addText(s.numbered === false ? it[2] || '✓' : String(i + 1), { x: x + 0.25, y: y + 0.25, w: 0.5, h: 0.5, fontSize: 16, bold: true, color: C.background1, align: 'center', valign: 'middle', margin: 0, isTextBox: true });
        slide.addText([{ text: it[0], options: { fontSize: 17, bold: true, color: C.text1, breakLine: true } }, { text: it[1], options: { fontSize: 13, color: C.accent4 } }],
          { x: x + 0.95, y: y + 0.2, w: cw - 1.15, h: ch - 0.4, valign: 'top', margin: 0, paraSpaceAfter: 3, isTextBox: true });
      });
    }
  }
  return pres;
}

/** PowerPoint 기본값은 한글을 글자 단위로 줄바꿈 → 단어 단위로 (문단 속성 eaLnBrk="0") */
async function koreanWordWrap(file) {
  const JSZip = require('jszip');
  const zip = await JSZip.loadAsync(fs.readFileSync(file));
  for (const name of Object.keys(zip.files).filter((n) => /^ppt\/(slides|slideLayouts|slideMasters)\/[^/]+\.xml$/.test(n))) {
    let xml = await zip.file(name).async('string');
    xml = xml.replace(/<a:pPr(?![^>]*eaLnBrk)/g, '<a:pPr eaLnBrk="0" latinLnBrk="0"')
             .replace(/<a:p>(?!<a:pPr)/g, '<a:p><a:pPr eaLnBrk="0" latinLnBrk="0"/>')
             .replace(/ lang="en-US"/g, ' lang="ko-KR" altLang="en-US"');
    zip.file(name, xml);
  }
  fs.writeFileSync(file, await zip.generateAsync({ type: 'nodebuffer', compression: 'DEFLATE' }));
}

async function main() {
  const decks = require('./content.js');
  for (const d of decks) {
    const pres = build(d);
    const file = path.join(OUT, d.file);
    await pres.writeFile({ fileName: file });
    await applyTheme(file, THEME);
    await koreanWordWrap(file);
    console.log('wrote', file);
  }
}
main().catch((e) => { console.error(e); process.exit(1); });
