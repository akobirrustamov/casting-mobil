/**
 * Panelda videoni ko'rish.
 *
 * <h2>⚠️ Qanday kamchilikni yopadi</h2>
 * Xodim video yuklardi, lekin uni HECH QACHON ko'ra olmasdi — panelda
 * pleyer umuman yo'q edi. Videoning buzuq emasligini tekshirishning
 * yagona yo'li kontentni nashr qilib, ilovadan ochish edi.
 *
 * <h2>⚠️ Nega manzil alohida so'raladi</h2>
 * Brauzerning {@code <video src>} elementi {@code Authorization}
 * sarlavhasini YUBORMAYDI. Shuning uchun server manzilning O'ZIGA
 * imzo qo'yib beradi va u har safar qayta so'raladi — imzo muddati
 * cheklangan.
 */
import { render, screen, waitFor } from '@testing-library/react';
import VideoPreview from '../VideoPreview';

jest.mock('../../api/client', () => ({
  adminApi: { mediaPreview: jest.fn() },
  mediaUrl: (id) => (id ? `/media/${id}` : null),
}));

const { adminApi } = require('../../api/client');

/** Tarjimani soddalashtiramiz — bu test matnni emas, xatti-harakatni sinaydi. */
const t = (key) => key;

beforeEach(() => {
  jest.clearAllMocks();
  adminApi.mediaPreview.mockReset();
});

describe('Videoni ko\'rish', () => {

  /**
   * ⚠️ ENG MUHIM TEKSHIRUV — pleyer serverdan kelgan MANZILNI
   * ishlatsin.
   *
   * Ilgari bunday manzil umuman yo'q edi va `<video>` ga oddiy
   * `/raw` berilsa 404 qaytarardi: element token yubormaydi.
   */
  it('Serverdan kelgan imzolangan manzil pleyerga beriladi', async () => {
    adminApi.mediaPreview.mockResolvedValue({
      mediaId: 7,
      url: 'https://s3.example/videos/7.mp4?X-Amz-Signature=abc',
      type: 'VIDEO',
    });

    const { container } = render(
      <VideoPreview open mediaId={7} title="Asosiy video" onClose={jest.fn()} t={t} />
    );

    await waitFor(() => {
      const video = container.querySelector('video');
      expect(video).toBeTruthy();
      expect(video.getAttribute('src'))
        .toBe('https://s3.example/videos/7.mp4?X-Amz-Signature=abc');
    });
  });

  /**
   * ⚠️ YOPIQ modal so'rov YUBORMAYDI.
   *
   * Qism muharririda o'nlab video maydoni bo'lishi mumkin. Har biri
   * sahifa ochilishida manzil so'rasa, imzo bekorga sarflanardi va
   * server ortiqcha yuk olardi.
   */
  it('Modal yopiq bo\'lsa so\'rov YUBORILMAYDI', () => {
    render(<VideoPreview open={false} mediaId={7} onClose={jest.fn()} t={t} />);

    expect(adminApi.mediaPreview).not.toHaveBeenCalled();
  });

  /**
   * ⚠️ Manzil HAR OCHILISHDA qayta so'raladi.
   *
   * U imzolangan va muddati cheklangan. Bir marta olib keshlansa,
   * bir necha soatdan keyin «video ochilmadi» bo'lardi va sababi
   * ko'rinmasdi.
   */
  it('Boshqa video ochilsa manzil QAYTA so\'raladi', async () => {
    adminApi.mediaPreview.mockResolvedValue({ mediaId: 7, url: 'https://s3/a.mp4' });

    const { rerender } = render(
      <VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />
    );
    await waitFor(() => expect(adminApi.mediaPreview).toHaveBeenCalledWith(7));

    adminApi.mediaPreview.mockResolvedValue({ mediaId: 9, url: 'https://s3/b.mp4' });
    rerender(<VideoPreview open mediaId={9} onClose={jest.fn()} t={t} />);

    await waitFor(() => expect(adminApi.mediaPreview).toHaveBeenCalledWith(9));
    expect(adminApi.mediaPreview).toHaveBeenCalledTimes(2);
  });

  /**
   * Manzil kelmasa xato KO'RSATILADI.
   *
   * ⚠️ Busiz modal bo'sh qora to'rtburchak bo'lib qolardi va admin
   * nimaga video ochilmayotganini bilmasdi.
   */
  it('Xato bo\'lsa sabab ko\'rsatiladi', async () => {
    adminApi.mediaPreview.mockRejectedValue({ message: 'Ruxsat yo\'q' });

    render(<VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />);

    await waitFor(() => {
      expect(screen.getByText(/Ruxsat yo'q/)).toBeInTheDocument();
    });
  });

  /**
   * ⚠️ `preload="metadata"` — butun fayl TORTILMAYDI.
   *
   * 600 MB lik manbada `preload="auto"` modal ochilishi bilan
   * yuzlab megabayt yuklardi.
   */
  it('Butun fayl oldindan yuklanmaydi', async () => {
    adminApi.mediaPreview.mockResolvedValue({ mediaId: 7, url: 'https://s3/a.mp4' });

    const { container } = render(
      <VideoPreview open mediaId={7} onClose={jest.fn()} t={t} />
    );

    await waitFor(() => {
      const video = container.querySelector('video');
      expect(video).toBeTruthy();
      expect(video.getAttribute('preload')).toBe('metadata');
    });
  });
});
