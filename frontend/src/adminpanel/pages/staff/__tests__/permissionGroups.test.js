import fs from 'fs';
import path from 'path';
import { ALL_PERMISSIONS, PERMISSION_GROUPS } from '../permissionGroups';

/**
 * Qo'riqchi test: paneldagi ruxsatlar ro'yxati backenddagi enum bilan
 * bir xilligini tekshiradi.
 *
 * <h2>Nima uchun kerak</h2>
 * WORKER huquqlari faqat shu ro'yxatdagi qutichalar orqali beriladi.
 * Backendga yangi ruxsat qo'shilsayu bu yerga qo'shilmasa, u panel
 * orqali HECH QACHON berib bo'lmaydigan huquqqa aylanadi — va buni
 * hech kim sezmaydi, chunki hech qayerda xato chiqmaydi: quticha
 * shunchaki yo'q bo'ladi.
 *
 * Teskarisi ham xavfli: bu yerda bor, backendda yo'q qiymat
 * yuborilsa, butun so'rov 400 bilan rad etiladi va admin sababini
 * tushunmaydi.
 */
const ENUM_PATH = path.resolve(
  __dirname,
  '../../../../../../backend/src/main/java/com/example/backend/Enums/Permission.java'
);

/** Java enum faylidan qiymatlarni ajratib oladi. */
function backendPermissions() {
  // ⚠️ Izohlar AVVAL tashlanadi, keyin enum tanasi qidiriladi.
  // Teskarisi ishlamasdi: sinf ustidagi javadoc ichida `{@link ...}`
  // bor va «birinchi ochilgan qavs» o'sha yerga tushib qolardi.
  const source = fs.readFileSync(ENUM_PATH, 'utf8')
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/\/\/.*$/gm, '');

  const start = source.indexOf('{', source.indexOf('enum Permission'));
  // Enum tanasi `;` bilan ham, to'g'ridan-to'g'ri `}` bilan ham
  // tugashi mumkin — Permission da metod yo'q, ya'ni `;` umuman yo'q.
  const rest = source.slice(start + 1);
  const semicolon = rest.indexOf(';');
  const brace = rest.indexOf('}');
  const end = semicolon === -1 ? brace : Math.min(semicolon, brace);

  return rest
    .slice(0, end)
    .split(',')
    .map((v) => v.trim())
    .filter((v) => /^[A-Z][A-Z0-9_]*$/.test(v));
}

describe('panel ruxsatlari backend enum bilan mos', () => {
  const backend = backendPermissions();

  test('backend enumi o\'qildi', () => {
    // Fayl topilmasa yoki tahlil buzilsa test jimgina o'tib ketmasin.
    expect(backend.length).toBeGreaterThan(30);
  });

  test('backenddagi har bir ruxsat panelda bor', () => {
    const missing = backend.filter((p) => !ALL_PERMISSIONS.includes(p));
    expect(missing).toEqual([]);
  });

  test('paneldagi har bir ruxsat backendda bor', () => {
    const extra = ALL_PERMISSIONS.filter((p) => !backend.includes(p));
    expect(extra).toEqual([]);
  });

  test('bitta ruxsat ikkita guruhga tushmagan', () => {
    const seen = new Set();
    const duplicates = [];
    PERMISSION_GROUPS.forEach((g) => g.permissions.forEach((p) => {
      if (seen.has(p)) duplicates.push(p);
      seen.add(p);
    }));
    expect(duplicates).toEqual([]);
  });
});
