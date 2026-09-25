/**
 * Yetim faylni panelda ko'rish.
 *
 * <h2>Nima bu yerda jim buziladi</h2>
 * Uchta narsa, va uchalasi ham skrinshotda to'g'ri ko'rinadi:
 *
 * 1. <b>Tugma yo'qolishi.</b> Ro'yxat to'la ko'rinaveradi, faqat
 *    faylni ko'rish imkoni yo'qoladi — admin yana ko'r-ko'rona
 *    o'chirishga qaytadi.
 * 2. <b>Tugma HAMMA yonida chiqishi.</b> HLS bo'lagi ({@code .m4s})
 *    o'zicha o'ynamaydi. Tugma bosilardi, pleyer ochilardi va hech
 *    narsa bo'lmasdi — admin «video buzuq» degan XATO xulosaga kelardi.
 * 3. <b>Rasm va video almashib ketishi.</b> Rasmni `<video>` ga
 *    berish bo'sh qora to'rtburchak chizardi; videoni `<img>` ga
 *    berish siniq rasm belgisini. Ikkalasi ham «fayl buzuq» degandek
 *    ko'rinardi, aslida fayl joyida.
 */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import StoragePage from '../StoragePage';
import { PanelI18nProvider } from '../../i18n';

jest.mock('../../api/client', () => ({
  adminApi: {
    storage: jest.fn(),
    storageScan: jest.fn(),
    storageBrowse: jest.fn(),
    storageDeleteOrphan: jest.fn(),
    storagePreview: jest.fn(),
    deleteMedia: jest.fn(),
  },
  BASE_URL: '',
}));

const { adminApi } = require('../../api/client');

const RASM = { key: 'cms-dev/afisha.jpg', sizeBytes: 120_000, previewKind: 'IMAGE' };
const VIDEO = { key: 'content/film.mp4', sizeBytes: 900_000_000, previewKind: 'VIDEO' };
const BOLAK = { key: 'videos/146/hls/480p/seg-1.m4s', sizeBytes: 500_000, previewKind: 'OTHER' };

function hisobot(orphans) {
  return {
    scannedAt: '2026-09-25T03:00:00Z',
    scanMillis: 363,
    complete: true,
    objectCount: 666,
    totalBytes: 4_772_000_000,
    folders: [],
    orphanCount: orphans.length,
    orphanBytes: orphans.reduce((a, o) => a + o.sizeBytes, 0),
    orphans,
    unusedAssetCount: 0,
    unusedAssetBytes: 0,
    unusedAssets: [],
    listLimit: 200,
  };
}

function ochish(orphans) {
  adminApi.storage.mockResolvedValue(hisobot(orphans));
  adminApi.storageBrowse.mockResolvedValue({ prefix: '', entries: [] });
  return render(
    <PanelI18nProvider>
      <StoragePage />
    </PanelI18nProvider>
  );
}

test("rasm va video yonida «Ko'rish» tugmasi bor", async () => {
  ochish([RASM, VIDEO]);

  await waitFor(() => expect(screen.getByText(RASM.key)).toBeInTheDocument());

  expect(screen.getAllByRole('button', { name: /Ko'rish/ })).toHaveLength(2);
});

test("⚠️ HLS bo'lagi yonida tugma CHIQMAYDI", async () => {
  ochish([BOLAK]);

  await waitFor(() => expect(screen.getByText(BOLAK.key)).toBeInTheDocument());

  // Qator bor, o'chirish tugmasi ham bor — faqat ko'rish yo'q.
  expect(screen.queryByRole('button', { name: /Ko'rish/ })).not.toBeInTheDocument();
  expect(screen.getAllByRole('button', { name: /Butunlay/ }).length).toBeGreaterThan(0);
});

test('rasm `img` bilan ko\'rsatiladi', async () => {
  const { container } = ochish([RASM]);
  adminApi.storagePreview.mockResolvedValue({
    key: RASM.key,
    url: 'https://s3.example.invalid/bucket/cms-dev/afisha.jpg?X-Amz-Signature=abc',
    contentType: 'image/jpeg',
    kind: 'IMAGE',
    expiresAt: new Date(Date.now() + 600_000).toISOString(),
  });

  await waitFor(() => expect(screen.getByText(RASM.key)).toBeInTheDocument());
  await userEvent.click(screen.getByRole('button', { name: /Ko'rish/ }));

  await waitFor(() => expect(adminApi.storagePreview).toHaveBeenCalledWith(RASM.key));

  const img = await screen.findByAltText('afisha.jpg');
  expect(img).toHaveAttribute('src', expect.stringContaining('X-Amz-Signature'));
  // ⚠️ Video elementi BO'LMASLIGI kerak: rasm `<video>` ga berilsa
  // bo'sh qora to'rtburchak chizilardi.
  expect(container.querySelector('video')).toBeNull();
});

test('video `video` elementi bilan o\'ynaydi', async () => {
  const { container } = ochish([VIDEO]);
  adminApi.storagePreview.mockResolvedValue({
    key: VIDEO.key,
    url: 'https://s3.example.invalid/bucket/content/film.mp4?X-Amz-Signature=xyz',
    contentType: 'video/mp4',
    kind: 'VIDEO',
    expiresAt: new Date(Date.now() + 600_000).toISOString(),
  });

  await waitFor(() => expect(screen.getByText(VIDEO.key)).toBeInTheDocument());
  await userEvent.click(screen.getByRole('button', { name: /Ko'rish/ }));

  await waitFor(() => expect(adminApi.storagePreview).toHaveBeenCalledWith(VIDEO.key));

  const video = await waitFor(() => {
    const el = container.querySelector('video');
    expect(el).not.toBeNull();
    return el;
  });

  // ⚠️ `controls` usiz pleyer ko'rinadi, lekin to'xtatib yoki oldinga
  // surib bo'lmasdi — ya'ni uzun videoni tekshirish imkonsiz bo'lardi.
  expect(video).toHaveAttribute('controls');
  // ⚠️ Butun faylni oldindan tortmasin: yetim video gigabaytlarcha
  // bo'lishi mumkin.
  expect(video).toHaveAttribute('preload', 'metadata');
});

test("havola xato bo'lsa sabab ko'rsatiladi", async () => {
  ochish([RASM]);
  adminApi.storagePreview.mockRejectedValue({
    response: { data: { message: 'Fayl topilmadi' } },
  });

  await waitFor(() => expect(screen.getByText(RASM.key)).toBeInTheDocument());
  await userEvent.click(screen.getByRole('button', { name: /Ko'rish/ }));

  // ⚠️ Xabar MATNI chiqishi shart. Faqat «⚠️» belgisi chiqsa, admin
  // nima bo'lganini bilmasdi.
  expect(await screen.findByText('Fayl topilmadi')).toBeInTheDocument();
});
