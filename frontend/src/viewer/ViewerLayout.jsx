import { Outlet } from 'react-router-dom';

import { ViewerI18nProvider } from './i18n';
import './theme/viewer.css';

/**
 * Tomoshabin yuzasining umumiy qobig'i: tarjima va palitra.
 *
 * <h2>Nima BOR va nima YO'Q</h2>
 * <pre>
 *   /kirish                → telefon + parol
 *   /tomosha/content/:id   → yaxlit kontent (film, klip)
 *   /tomosha/episode/:id   → serialning bitta qismi
 * </pre>
 *
 * Katalog, bosh sahifa va qidiruv bu yerda YO'Q — sahifaga havola
 * orqali kiriladi. Ular mobil ilovada bor va web'ga ko'chirish
 * alohida ish.
 *
 * <h2>⚠️ Yo'lda `content`/`episode` ATAYLAB ko'rinadi</h2>
 * Ko'rish backendda ikki xil endpointdan boradi va identifikatorlar
 * mustaqil nomerlanadi: `/tomosha/7` ikki xil videoni anglatishi
 * mumkin edi va qaysi biri kerakligini aniqlashning iloji bo'lmasdi.
 *
 * <h2>⚠️ Nega yo'lsiz (pathless) marshrut</h2>
 * Dastlab bu yerda o'z ichida {@code <Routes>} bo'lgan komponent
 * turgan edi. U ishlamaydi: ota-marshrut {@code /kirish} ni to'liq
 * «yeb» qo'yadi va ichkarida solishtiradigan yo'l qolmaydi —
 * sahifa bo'sh chiqardi.
 *
 * Yo'lsiz marshrut esa manzilga umuman tegmaydi: u faqat qobiq
 * beradi, bolalari esa {@code App.js} da to'liq yo'llari bilan
 * turadi va manzillar qisqa qoladi.
 *
 * <h2>⚠️ Panel bilan hech narsa umumiy emas</h2>
 * Alohida API klienti, alohida token, alohida tarjima va alohida
 * palitra. Umumiy narsa faqat marshrutlar daraxti — shuning uchun
 * bitta brauzerda ikkala hisobga birdan kirib turish mumkin.
 */
export default function ViewerLayout() {
  return (
    <ViewerI18nProvider>
      <Outlet />
    </ViewerI18nProvider>
  );
}
