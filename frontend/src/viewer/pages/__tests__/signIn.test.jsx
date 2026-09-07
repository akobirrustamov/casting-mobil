/**
 * Tomoshabin kirishi — SMS kod orqali, uch qadamda.
 *
 * <h2>⚠️ `next` parametri — xavfsizlik masalasi</h2>
 * Kirishdan keyin qayerga o'tish manzildan olinadi. Tashqi manzil
 * qabul qilinsa, havola bilan odamni begona saytga olib chiqish
 * mumkin bo'lardi — va aynan kirishdan keyin, ya'ni u eng ishonchli
 * kayfiyatda bo'lgan lahzada.
 *
 * Shuning uchun faqat ICHKI yo'l qabul qilinadi.
 *
 * <h2>⚠️ Nega parol emas</h2>
 * Sahifa ilgari telefon va parol yuborardi. O'sha endpoint
 * backenddan olib tashlangan va saytdagi kirish umuman ishlamay
 * qolgan edi — nosozlik esa jimgina, «parol xato» bo'lib
 * ko'rinardi. Shuning uchun bu yerda endi uchta qadam sinaladi.
 */
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('../../api/client', () => ({
  sendCode: jest.fn(),
  verifyCode: jest.fn(),
  completeSignUp: jest.fn(),
}));

/**
 * ⚠️ `useNavigate` ushlanadi — bu YAGONA ishonchli tekshiruv.
 *
 * Avval bu testlar `window.history.pushState` ni kuzatardi. Ular
 * DOIM o'tardi va hech narsani tekshirmasdi: `MemoryRouter`
 * `window.history` ga umuman tegmaydi — manzilni xotirada saqlaydi.
 *
 * Mutatsiya buni ochib berdi: tashqi manzil tekshiruvini butunlay
 * olib tashlaganimda ham testlar yashil turdi.
 */
const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('../../i18n', () => ({
  useViewerI18n: () => ({
    // Kalitni o'zini qaytaramiz, lekin o'rniga qo'yish ishlasin.
    t: (key, vars) =>
      (vars ? `${key}:${Object.values(vars).join(',')}` : key),
    locale: 'uz',
    setLocale: () => {},
  }),
}));

const { sendCode, verifyCode, completeSignUp } = require('../../api/client');

const SignInPage = require('../SignInPage').default;

/** Qayerga o'tganini ko'rsatadigan soxta sahifa. */
function Manzil() {
  return <div>MANZIL</div>;
}

function renderAt(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/kirish" element={<SignInPage />} />
        <Route path="*" element={<Manzil />} />
      </Routes>
    </MemoryRouter>
  );
}

/** 1-qadam: raqamni kiritib «kod olish». */
async function enterPhone(phone = '+998945434230') {
  fireEvent.change(screen.getByRole('textbox'), { target: { value: phone } });
  fireEvent.click(screen.getByRole('button', { name: 'signIn.submit' }));
  await screen.findByRole('button', { name: 'signIn.codeSubmit' });
}

/** 2-qadam: kodni kiritib tasdiqlash. */
async function enterCode(code = '123456') {
  fireEvent.change(screen.getByRole('textbox'), { target: { value: code } });
  fireEvent.click(screen.getByRole('button', { name: 'signIn.codeSubmit' }));
}

/** Ikkala qadam — eski foydalanuvchi yo'li. */
async function signInFully() {
  await enterPhone();
  await enterCode();
}

beforeEach(() => {
  jest.clearAllMocks();
  mockNavigate.mockReset();
  sendCode.mockReset().mockResolvedValue({ expiresInSeconds: 0 });
  verifyCode.mockReset().mockResolvedValue({ nameRequired: false });
  completeSignUp.mockReset().mockResolvedValue({});
});

// ------------------------------------------------------------------ oqim

it('Raqam kiritilgach kod so‘raladi', async () => {
  renderAt('/kirish');
  await enterPhone();

  expect(sendCode).toHaveBeenCalledWith('+998945434230');
  expect(screen.getByText('signIn.codeTitle')).toBeInTheDocument();
});

it('Kod to‘g‘ri bo‘lsa ichkariga kiradi', async () => {
  renderAt('/kirish');
  await signInFully();

  await waitFor(() =>
    expect(verifyCode).toHaveBeenCalledWith('+998945434230', '123456')
  );
});

/**
 * ⚠️ Yangi odam uchun UCHINCHI qadam bor.
 *
 * `name_required` javobini e'tiborsiz qoldirsak, odam tokensiz
 * «kirgan» bo'lardi: ekran ochilardi, lekin har bir so'rov 401
 * berardi va nima bo'layotgani tushunarsiz bo'lardi.
 */
it('Yangi foydalanuvchidan ism so‘raladi', async () => {
  verifyCode.mockResolvedValue({ nameRequired: true });

  renderAt('/kirish');
  await signInFully();

  expect(await screen.findByText('signIn.nameTitle')).toBeInTheDocument();
  expect(mockNavigate).not.toHaveBeenCalled();
});

it('Ism yuborilgach ichkariga kiradi', async () => {
  verifyCode.mockResolvedValue({ nameRequired: true });

  renderAt('/kirish');
  await signInFully();
  await screen.findByText('signIn.nameTitle');

  fireEvent.change(screen.getByRole('textbox'), { target: { value: 'Yangi Odam' } });
  fireEvent.click(screen.getByRole('button', { name: 'signIn.nameSubmit' }));

  await waitFor(() =>
    expect(completeSignUp).toHaveBeenCalledWith('+998945434230', 'Yangi Odam')
  );
  await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/'));
});

/**
 * ⚠️ Har bosish HAQIQIY SMS va haqiqiy pul.
 *
 * Sanoq ishlayotganda tugma o'chiq turishi shart: aks holda sabrsiz
 * odam uni ketma-ket bosib, bir necha xabar yuborib yuborardi.
 */
it('Qayta yuborish sanoq tugagunicha o‘chiq', async () => {
  sendCode.mockResolvedValue({ expiresInSeconds: 60 });

  renderAt('/kirish');
  await enterPhone();

  expect(screen.getByRole('button', { name: /signIn.resendIn/ })).toBeDisabled();
});

it('Raqamni o‘zgartirishga qaytish mumkin', async () => {
  renderAt('/kirish');
  await enterPhone();

  fireEvent.click(screen.getByRole('button', { name: 'signIn.back' }));

  expect(await screen.findByText('signIn.hint')).toBeInTheDocument();
});

// ------------------------------------------------------- qaytish manzili

/**
 * ⚠️ Odam kirish tugmasini VIDEO uchun bosgan.
 *
 * `next` siz u ildizga tushardi — u yerda esa boshqa mahsulot, eski
 * casting sayti — va videoni qaytadan qidirishga majbur bo'lardi.
 */
it('Kirishdan keyin `next` manziliga qaytaradi', async () => {
  renderAt('/kirish?next=%2Ftomosha%2Fcontent%2F13');
  await signInFully();

  await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/tomosha/content/13'));
});

/** `next` yo'q — ildizga. Boshqa boradigan joy yo'q. */
it('`next` bo‘lmasa ildizga', async () => {
  renderAt('/kirish');
  await signInFully();

  await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/'));
});

/**
 * ⚠️ TASHQI manzil rad etiladi.
 *
 * Aks holda `/kirish?next=https://soxta.example` degan havola odamni
 * kirishdan darhol keyin begona saytga olib chiqardi.
 */
it('Tashqi manzilga o‘tkazmaydi', async () => {
  renderAt('/kirish?next=https%3A%2F%2Fsoxta.example%2Fkirish');
  await signInFully();

  await waitFor(() => expect(mockNavigate).toHaveBeenCalled());
  expect(mockNavigate).toHaveBeenCalledWith('/');
  expect(mockNavigate).not.toHaveBeenCalledWith(
    expect.stringContaining('soxta.example')
  );
});

/**
 * ⚠️ Protokolsiz `//boshqa-sayt` HAM tashqi manzil.
 *
 * U `/` bilan boshlanadi, ya'ni sodda tekshiruvdan o'tib ketardi —
 * brauzer esa uni boshqa domen deb tushunadi.
 */
it('`//` bilan boshlanuvchi manzil ham rad etiladi', async () => {
  renderAt('/kirish?next=%2F%2Fsoxta.example');
  await signInFully();

  await waitFor(() => expect(mockNavigate).toHaveBeenCalled());
  expect(mockNavigate).toHaveBeenCalledWith('/');
  expect(mockNavigate).not.toHaveBeenCalledWith(
    expect.stringContaining('soxta.example')
  );
});

// ------------------------------------------------------------- xatolar

it('Xato kodda aniq xabar chiqadi', async () => {
  const err = new Error('401');
  err.response = { status: 401 };
  verifyCode.mockRejectedValue(err);

  renderAt('/kirish');
  await signInFully();

  expect(await screen.findByText('error.credentials')).toBeInTheDocument();
});

/**
 * ⚠️ Cheklovga urilgan odam «kod noto'g'ri» xabarini ko'rmasin.
 *
 * U to'g'ri kodni qayta-qayta kiritib, har safar yangi cheklovga
 * urilardi va nima bo'layotganini tushunmasdi.
 */
it('Cheklovda alohida xabar chiqadi', async () => {
  const err = new Error('429');
  err.response = { status: 429 };
  verifyCode.mockRejectedValue(err);

  renderAt('/kirish');
  await signInFully();

  expect(await screen.findByText('error.tooMany')).toBeInTheDocument();
});

/**
 * ⚠️ Tarmoq xatosi «kod noto'g'ri» deb ko'rsatilmasin — odam
 * to'g'ri kodni qayta-qayta kiritishga urinardi.
 */
it('Tarmoq xatosi boshqacha xabar beradi', async () => {
  verifyCode.mockRejectedValue(new Error('tarmoq'));

  renderAt('/kirish');
  await signInFully();

  expect(await screen.findByText('error.network')).toBeInTheDocument();
});

/** Raqam qadamida xato — kod haqida emas, raqam haqida gapiriladi. */
it('Noto‘g‘ri raqamda raqam haqida xabar chiqadi', async () => {
  const err = new Error('422');
  err.response = { status: 422 };
  sendCode.mockRejectedValue(err);

  renderAt('/kirish');
  fireEvent.change(screen.getByRole('textbox'), { target: { value: 'xxx' } });
  fireEvent.click(screen.getByRole('button', { name: 'signIn.submit' }));

  expect(await screen.findByText('error.phone')).toBeInTheDocument();
});
