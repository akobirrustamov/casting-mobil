import { render, screen } from '@testing-library/react';
import TranscodingQueue from '../TranscodingQueue';
import { PanelI18nProvider } from '../../i18n';

/**
 * Navbat va server holati banneri.
 *
 * <h2>⚠️ Nima qo'riqlanadi</h2>
 * FFmpeg o'rnatilmagan serverda har bir video uch marta urinib
 * yiqilardi va admin buzuq fayl izlab yurardi — sabab esa serverda
 * edi.
 *
 * Bu banner javobni bir marta beradi. Agar u ko'rinmay qolsa,
 * nosozlik yana jimgina bo'lardi.
 */

/** Komponent tarjimalarni kontekstdan oladi. */
const show = (queue) => render(
  <PanelI18nProvider>
    <TranscodingQueue queue={queue} />
  </PanelI18nProvider>,
);

const healthy = {
  queued: 0,
  running: 0,
  failed: 0,
  system: { problems: [], freeDiskBytes: 120 * 1024 * 1024 * 1024 },
};

describe('Navbat banneri', () => {
  /**
   * ⚠️ Doim ko'rinadigan «0 · 0 · 0» qatori shovqin bo'lardi va bir
   * hafta ichida odam unga umuman qaramay qo'yardi — aynan shunda u
   * kerak bo'lganda ham ko'rinmasdi.
   */
  it('hammasi joyida bo\'lsa HECH NARSA chizmaydi', () => {
    const { container } = show(healthy);
    expect(container).toBeEmptyDOMElement();
  });

  it('ma\'lumot yuklanmagan bo\'lsa chizmaydi', () => {
    const { container } = show(null);
    expect(container).toBeEmptyDOMElement();
  });

  /** ⚠️ ENG MUHIM TEKSHIRUV — bu holat uchun komponent yozilgan. */
  it('server muammosini KO\'RSATADI', () => {
    show({
      ...healthy,
      system: { problems: ['FFmpeg topilmadi (`ffmpeg`)'], freeDiskBytes: 1 },
    });

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText(/FFmpeg topilmadi/)).toBeInTheDocument();
  });

  /**
   * ⚠️ Admin serverga kira olmaydi. «FFmpeg yo'q» degan xabar undan
   * hech qanday amal talab qilmaydi — u kimga murojaat qilishni
   * bilishi kerak.
   */
  it('nima qilish kerakligini aytadi', () => {
    show({
      ...healthy,
      system: { problems: ['FFmpeg topilmadi'], freeDiskBytes: 1 },
    });

    expect(screen.getByText(/administrator/i)).toBeInTheDocument();
  });

  it('bir nechta muammoni ham ko\'rsatadi', () => {
    show({
      ...healthy,
      system: { problems: ['FFmpeg topilmadi', 'ffprobe topilmadi'], freeDiskBytes: 1 },
    });

    expect(screen.getByText(/^FFmpeg topilmadi$/)).toBeInTheDocument();
    expect(screen.getByText(/^ffprobe topilmadi$/)).toBeInTheDocument();
  });

  it('navbatdagi ishlar sonini ko\'rsatadi', () => {
    show({ ...healthy, queued: 3, running: 1 });

    expect(screen.getByText(/3/)).toBeInTheDocument();
    expect(screen.getByText(/Bajarilmoqda: 1/)).toBeInTheDocument();
  });

  /** Yiqilganlar ish tugagach ham ko'rinishi kerak — admin ularni topsin. */
  it('ish yo\'q bo\'lsa ham yiqilganlarni ko\'rsatadi', () => {
    show({ ...healthy, failed: 2 });

    expect(screen.getByText(/Yiqilgan: 2/)).toBeInTheDocument();
  });

  it('nol qiymatli nishonlar chizilmaydi', () => {
    show({ ...healthy, queued: 2 });

    expect(screen.queryByText(/Bajarilmoqda/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Yiqilgan/)).not.toBeInTheDocument();
  });

  it('bo\'sh joyni GB da ko\'rsatadi', () => {
    show({
      ...healthy,
      queued: 1,
      system: { problems: [], freeDiskBytes: 50 * 1024 * 1024 * 1024 },
    });

    expect(screen.getByText(/50\.0 GB/)).toBeInTheDocument();
  });

  /**
   * ⚠️ `null` — «o'lchab bo'lmadi», NOL emas.
   *
   * Nol ko'rsatilsa admin mavjud bo'lmagan «disk to'ldi» muammosini
   * tuzatishga urinardi.
   */
  it('o\'lchanmagan disk KO\'RSATILMAYDI', () => {
    show({
      ...healthy,
      queued: 1,
      system: { problems: [], freeDiskBytes: null },
    });

    expect(screen.queryByText(/GB/)).not.toBeInTheDocument();
  });

  /** Backend eski bo'lsa `system` umuman kelmasligi mumkin. */
  it('`system` bo\'lmasa ham yiqilmaydi', () => {
    show({ queued: 1, running: 0, failed: 0 });

    expect(screen.getByText(/Navbatda: 1/)).toBeInTheDocument();
  });
});
