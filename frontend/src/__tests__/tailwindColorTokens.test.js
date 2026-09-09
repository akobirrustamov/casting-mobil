/**
 * Tailwind rang sinflari haqiqatan mavjudmi.
 *
 * <h2>⚠️ Nega bu test yozildi</h2>
 * 404 sahifasidagi katta «404» yozuvi jonli saytda BUTUNLAY
 * ko'rinmasdi. Sabab:
 *
 * <pre>
 *   className="bg-gradient-to-b from-primary to-secondary
 *              bg-clip-text text-transparent"
 * </pre>
 *
 * `primary` va `secondary` ranglari `tailwind.config.js` da umuman
 * aniqlanmagan. Tailwind noma'lum sinfni shunchaki CHIQARIB
 * TASHLAYDI — na xato, na ogohlantirish. Natijada brauzerda
 * `linear-gradient(transparent, transparent)` qoldi, matn esa
 * `text-transparent` bo'lgani uchun `rgba(0,0,0,0)` bo'lib ketdi.
 *
 * Ya'ni sahifadagi eng katta element ko'rinmay qoldi va buni faqat
 * odam ochib ko'rgandagina bilish mumkin edi.
 *
 * ⚠️ `text-transparent` bilan birga kelgan gradient ayniqsa xavfli:
 * oddiy holatda noto'g'ri rang shunchaki qora bo'lib qolardi va
 * ko'zga tashlanardi. Bu yerda esa element butunlay yo'qoladi.
 */
const fs = require('fs');
const path = require('path');

const SRC = path.join(__dirname, '..');
const CONFIG = path.join(__dirname, '..', '..', 'tailwind.config.js');

/** Tailwind'ning o'zida bor ranglar — bularni tekshirish shart emas. */
const BUILT_IN = new Set([
  'inherit', 'current', 'transparent', 'black', 'white',
  'slate', 'gray', 'zinc', 'neutral', 'stone',
  'red', 'orange', 'amber', 'yellow', 'lime', 'green', 'emerald',
  'teal', 'cyan', 'sky', 'blue', 'indigo', 'violet', 'purple',
  'fuchsia', 'pink', 'rose',
]);

/** Rang KUTILADIGAN prefikslar. `to-` chetda: `duration-`, `w-` ham shunday boshlanmaydi. */
const PREFIXES = ['from', 'via', 'to', 'bg', 'text', 'border', 'ring', 'fill', 'stroke', 'shadow', 'decoration'];

function sourceFiles(dir, out = []) {
  for (const name of fs.readdirSync(dir)) {
    const p = path.join(dir, name);
    const st = fs.statSync(p);
    if (st.isDirectory()) {
      if (name !== 'node_modules' && name !== '__tests__') sourceFiles(p, out);
    } else if (/\.(js|jsx|ts|tsx)$/.test(name)) {
      out.push(p);
    }
  }
  return out;
}

/** `theme.extend.colors` da aniqlangan nomlar. */
function configuredColors() {
  const src = fs.readFileSync(CONFIG, 'utf8');
  const block = src.match(/colors\s*:\s*\{([\s\S]*?)\n\s{6}\}/);
  if (!block) return new Set();
  return new Set([...block[1].matchAll(/^\s*["']?([a-zA-Z][\w-]*)["']?\s*:/gm)].map((m) => m[1]));
}

it('Ishlatilgan rang sinflari haqiqatan aniqlangan', () => {
  const known = new Set([...BUILT_IN, ...configuredColors()]);
  const bad = [];

  // `bg-primary/10`, `from-primary`, `text-secondary` kabi.
  const re = new RegExp(`\\b(${PREFIXES.join('|')})-([a-z][a-z0-9-]*)(?:\\/\\d+)?\\b`, 'g');

  for (const file of sourceFiles(SRC)) {
    // ⚠️ Izohlar tashlab yuboriladi. Aks holda xatoni TUSHUNTIRGAN
    // izoh ("ilgari bu yerda `from-primary` turgandi") testni
    // yiqitardi — ya'ni sababni yozib qo'yish taqiqlangan bo'lardi.
    const text = fs.readFileSync(file, 'utf8')
      .replace(/\/\*[\s\S]*?\*\//g, '')
      .replace(/^\s*\/\/.*$/gm, '');

    for (const m of text.matchAll(re)) {
      const name = m[2];
      // Raqam bilan tugaydigan (`blue-600`) — asosiy nom oldida turadi.
      const base = name.replace(/-\d+$/, '');
      // Rang bo'lmagan yordamchi sinflar (`text-sm`, `bg-cover`, `to-do`) —
      // ular BUILT_IN da ham, sozlamada ham yo'q, lekin rang emas.
      // Shuning uchun faqat AYNAN shubhali nomlarni tekshiramiz: agar
      // nom sozlamada rang sifatida kutilgan bo'lsa-yu, topilmasa.
      if (/^(primary|secondary|accent|brand|surface|muted)$/.test(base) && !known.has(base)) {
        bad.push(`${path.relative(SRC, file)} → ${m[0]}`);
      }
    }
  }

  expect(bad).toEqual([]);
});

/**
 * ⚠️ Qoida haqiqatan yiqila olishiga ishonch.
 *
 * Fayllar topilmasa yoki ifoda hech narsaga mos kelmasa, yuqoridagi
 * test HAR DOIM yashil bo'lardi — hech narsa tekshirmay turib.
 */
it('Qoida haqiqatan yiqila oladi', () => {
  expect(sourceFiles(SRC).length).toBeGreaterThan(20);
  expect(configuredColors().size + BUILT_IN.size).toBeGreaterThan(20);

  const re = new RegExp(`\\b(${PREFIXES.join('|')})-([a-z][a-z0-9-]*)(?:\\/\\d+)?\\b`, 'g');
  expect([...'from-primary to-secondary'.matchAll(re)].map((m) => m[2]))
    .toEqual(['primary', 'secondary']);
});
