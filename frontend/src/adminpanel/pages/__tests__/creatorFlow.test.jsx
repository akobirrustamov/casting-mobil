/**
 * ТЗ §86 — kritik oqim: IJODKOR YARATISH.
 *
 * <h2>Nima uchun aynan bu tekshiriladi</h2>
 * Ijodkor formasi uch tilli va uning ikkita jimgina buziladigan
 * xatti-harakati bor:
 *
 * 1. O'zbekcha ism majburiy. Bo'sh qolsa forma saqlanmasligi va
 *    foydalanuvchini UZ tabiga QAYTARISHI kerak — aks holda admin
 *    inglizcha tabda turib «nega saqlanmayapti?» deb qolardi.
 * 2. Uchala til bitta so'rovda ketishi kerak. Ilgari tarjima
 *    yangilanishida `UNIQUE(parent, locale)` buzilardi (B12), ya'ni bu
 *    yo'nalish tarixan sinovchan.
 *
 * ⚠️ Bu test SERVERNI tekshirmaydi — `adminApi` almashtiriladi.
 * Backend tomoni `CreatorModuleTest` da qamralgan. Bu yerda faqat
 * forma serverga NIMA yuborishini tekshiramiz.
 */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CreatorForm from '../CreatorForm';
import { PanelI18nProvider } from '../../i18n';

jest.mock('../../api/client', () => ({
  adminApi: {
    createCreator: jest.fn(),
    updateCreator: jest.fn(),
    // `MediaField` kutubxonani ochganda chaqiradi — bo'sh ro'yxat yetarli.
    media: jest.fn().mockResolvedValue({ items: [], totalPages: 0, totalItems: 0 }),
    uploadMedia: jest.fn(),
  },
  mediaUrl: (id) => (id ? `/media/${id}` : null),
}));

const { adminApi } = require('../../api/client');

function setup(props = {}) {
  const onSaved = jest.fn();
  const onClose = jest.fn();
  render(
    <PanelI18nProvider>
      <CreatorForm open row={null} onSaved={onSaved} onClose={onClose} {...props} />
    </PanelI18nProvider>
  );
  return { onSaved, onClose };
}

/** Faol tildagi ism maydonlari. Tab almashgach bir xil `id` qoladi. */
const firstName = () => document.getElementById('cr-fn');
const lastName = () => document.getElementById('cr-ln');
const displayName = () => document.getElementById('cr-dn');
const saveButton = () => screen.getByRole('button', { name: /saqlash/i });

beforeEach(() => {
  jest.clearAllMocks();
  adminApi.createCreator.mockResolvedValue({ id: 7 });
});

describe("Ijodkor yaratish (§86)", () => {
  test("o'zbekcha ism bo'sh bo'lsa so'rov YUBORILMAYDI", async () => {
    const user = userEvent.setup();
    setup();

    await user.click(saveButton());

    expect(adminApi.createCreator).not.toHaveBeenCalled();
    // Xato ko'rinishi shart: jim qolish «saqlandi» degan taassurot berardi.
    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });

  test('uchala til bitta so\'rovda ketadi', async () => {
    const user = userEvent.setup();
    const { onSaved, onClose } = setup();

    await user.type(firstName(), 'Alisher');
    await user.type(lastName(), 'Karimov');

    // RU tabiga o'tib to'ldiramiz.
    //
    // ⚠️ `role` — `tab`, `button` emas (`LocaleTabs`). Va nom faqat
    // «RU» emas: to'ldirilmagan tilda qizil nuqtaning `aria-label` i
    // ham nomga qo'shiladi, shuning uchun boshidan moslashtiramiz.
    await user.click(screen.getByRole('tab', { name: /^RU/ }));
    await user.type(firstName(), 'Алишер');
    await user.type(lastName(), 'Каримов');

    await user.click(screen.getByRole('tab', { name: /^EN/ }));
    await user.type(displayName(), 'Alisher Karimov');

    await user.click(saveButton());

    await waitFor(() => expect(adminApi.createCreator).toHaveBeenCalledTimes(1));

    const payload = adminApi.createCreator.mock.calls[0][0];
    expect(payload.translations.UZ).toMatchObject({
      firstName: 'Alisher', lastName: 'Karimov',
    });
    expect(payload.translations.RU).toMatchObject({
      firstName: 'Алишер', lastName: 'Каримов',
    });
    expect(payload.translations.EN).toMatchObject({ displayName: 'Alisher Karimov' });

    // Muvaffaqiyatli saqlashdan keyin ro'yxat yangilanadi va oyna yopiladi.
    expect(onSaved).toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
  });

  test("displayName bo'sh bo'lsa ham ism+familiya UZ ni to'ldirilgan hisoblaydi", async () => {
    const user = userEvent.setup();
    setup();

    // Faqat ism — `displayName` yo'q. Backend uni o'zi yig'adi.
    await user.type(firstName(), 'Nodira');
    await user.click(saveButton());

    await waitFor(() => expect(adminApi.createCreator).toHaveBeenCalledTimes(1));
    expect(adminApi.createCreator.mock.calls[0][0].translations.UZ.firstName)
      .toBe('Nodira');
  });

  test('backend maydon xatosi MAYDON yoniga bog\'lanadi (§52)', async () => {
    const user = userEvent.setup();
    adminApi.createCreator.mockRejectedValue({
      status: 400,
      code: 'VALIDATION_ERROR',
      message: 'Kiritilgan ma\'lumot noto\'g\'ri',
      errors: [{ field: 'translations[UZ].displayName', message: 'Ism juda qisqa' }],
    });
    setup();

    await user.type(firstName(), 'A');
    await user.click(saveButton());

    // Umumiy «Validatsiya xatosi» emas, AYNAN maydon ostidagi sabab.
    expect(await screen.findByText('Ism juda qisqa')).toBeInTheDocument();
  });
});
