/**
 * Video qayta ishlash nishoni.
 *
 * <h2>Nima qo'riqlanadi</h2>
 * <ul>
 *   <li>rasm va hujjat uchun nishon UMUMAN chizilmaydi — `transcoding`
 *       u yerda `null` va bo'sh nishon «rasm qayta ishlanmoqda» degan
 *       ma'nosiz holatni ko'rsatardi;</li>
 *   <li>eski, ishi yo'q video uchun ham chizilmaydi;</li>
 *   <li>backend yangi holat qo'shsa panel YIQILMAYDI;</li>
 *   <li>progress faqat ishlash paytida va noldan katta bo'lsa.</li>
 * </ul>
 */
import { render, screen } from '@testing-library/react';
import TranscodingBadge from '../TranscodingBadge';
import { PanelI18nProvider } from '../../i18n';

function wrap(transcoding, props = {}) {
  return render(
    <PanelI18nProvider>
      <TranscodingBadge transcoding={transcoding} {...props} />
    </PanelI18nProvider>,
  );
}

describe('Nishon chizilmaydigan holatlar', () => {
  /**
   * ⚠️ `null` — «bu savol tegishli emas» (rasm, hujjat) yoki «eski
   * fayl, ishi yo'q». Ikkalasi ham nishonsiz qoladi.
   */
  test('`transcoding` null bo\'lsa hech narsa chizilmaydi', () => {
    const { container } = wrap(null);
    expect(container).toBeEmptyDOMElement();
  });

  test('`transcoding` undefined bo\'lsa ham', () => {
    const { container } = wrap(undefined);
    expect(container).toBeEmptyDOMElement();
  });

  /**
   * ⚠️ Backend yangi holat qo'shishi mumkin (masalan `PAUSED`).
   * Panel eski versiyada qolgan bo'lsa u yiqilmasligi kerak —
   * noma'lum qiymat shunchaki ko'rsatilmaydi.
   */
  test('NOMA\'LUM holat panelni yiqitmaydi', () => {
    const { container } = wrap({ status: 'KELAJAKDAGI_HOLAT', progress: 50 });
    expect(container).toBeEmptyDOMElement();
  });
});

describe('Holatlar', () => {
  test.each([
    ['QUEUED', 'Navbatda'],
    ['READY', 'Video tayyor'],
    ['FAILED', 'Yiqildi'],
  ])('%s → «%s»', (status, label) => {
    wrap({ status, progress: 0 });
    expect(screen.getByText(label)).toBeInTheDocument();
  });

  test('ishlash paytida progress QO\'SHILADI', () => {
    wrap({ status: 'TRANSCODING', progress: 62 });
    expect(screen.getByText(/62%/)).toBeInTheDocument();
  });

  /**
   * ⚠️ «0%» hech qanday ma'lumot bermaydi, faqat joy egallaydi va
   * adminda «to'xtab qoldi» degan taassurot uyg'otadi.
   */
  test('progress NOL bo\'lsa foiz ko\'rsatilmaydi', () => {
    wrap({ status: 'TRANSCODING', progress: 0 });
    expect(screen.queryByText(/0%/)).not.toBeInTheDocument();
    expect(screen.getByText('Qayta ishlanmoqda')).toBeInTheDocument();
  });

  /**
   * ⚠️ Tayyor videoda progress ko'rsatish ortiqcha: `READY` ning o'zi
   * hamma narsani aytadi.
   */
  test('READY holatida progress ko\'rsatilmaydi', () => {
    wrap({ status: 'READY', progress: 100 });
    expect(screen.queryByText(/100%/)).not.toBeInTheDocument();
  });

  test('navbatdagi ish uchun ham progress yo\'q', () => {
    wrap({ status: 'QUEUED', progress: 0 });
    expect(screen.getByText('Navbatda')).toBeInTheDocument();
    expect(screen.queryByText(/%/)).not.toBeInTheDocument();
  });
});

describe('Kartochka ko\'rinishi', () => {
  /**
   * Kartochka tor — u yerda faqat foiz ko'rsatiladi, to'liq matn
   * sig'maydi va kesilib qolardi.
   */
  test('`compact` da faqat foiz chiqadi', () => {
    wrap({ status: 'TRANSCODING', progress: 45 }, { compact: true });
    expect(screen.getByText('45%')).toBeInTheDocument();
    expect(screen.queryByText(/Qayta ishlanmoqda/)).not.toBeInTheDocument();
  });

  test('`compact` da foiz bo\'lmasa TO\'LIQ matn chiqadi', () => {
    wrap({ status: 'FAILED', progress: 0 }, { compact: true });
    expect(screen.getByText('Yiqildi')).toBeInTheDocument();
  });
});
