/**
 * Tomoshabin kirishi.
 *
 * <h2>⚠️ `next` parametri — xavfsizlik masalasi</h2>
 * Kirishdan keyin qayerga o'tish manzildan olinadi. Tashqi manzil
 * qabul qilinsa, havola bilan odamni begona saytga olib chiqish
 * mumkin bo'lardi — va aynan kirishdan keyin, ya'ni u eng ishonchli
 * kayfiyatda bo'lgan lahzada.
 *
 * Shuning uchun faqat ICHKI yo'l qabul qilinadi.
 */
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('../../api/client', () => ({ signIn: jest.fn() }));

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
  useViewerI18n: () => ({ t: (key) => key, locale: 'uz', setLocale: () => {} }),
}));

const { signIn } = require('../../api/client');

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

async function submit() {
  const [tel, parol] = screen.getAllByRole('textbox').concat(
    document.querySelectorAll('input[type="password"]')[0]
  );
  fireEvent.change(tel, { target: { value: '+998945434230' } });
  fireEvent.change(parol, { target: { value: 'akow8434' } });
  fireEvent.click(screen.getByRole('button', { name: 'signIn.submit' }));
}

beforeEach(() => {
  jest.clearAllMocks();
  mockNavigate.mockReset();
  signIn.mockReset();
  signIn.mockResolvedValue({});
});

it('Kirish so‘rovi telefon va parol bilan ketadi', async () => {
  renderAt('/kirish');
  await submit();

  await waitFor(() =>
    expect(signIn).toHaveBeenCalledWith('+998945434230', 'akow8434')
  );
});

/**
 * ⚠️ Odam kirish tugmasini VIDEO uchun bosgan.
 *
 * `next` siz u ildizga tushardi — u yerda esa boshqa mahsulot, eski
 * casting sayti — va videoni qaytadan qidirishga majbur bo'lardi.
 */
it('Kirishdan keyin `next` manziliga qaytaradi', async () => {
  renderAt('/kirish?next=%2Ftomosha%2Fcontent%2F13');
  await submit();

  await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/tomosha/content/13'));
});

/** `next` yo'q — ildizga. Boshqa boradigan joy yo'q. */
it('`next` bo‘lmasa ildizga', async () => {
  renderAt('/kirish');
  await submit();

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
  await submit();

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
  await submit();

  await waitFor(() => expect(mockNavigate).toHaveBeenCalled());
  expect(mockNavigate).toHaveBeenCalledWith('/');
  expect(mockNavigate).not.toHaveBeenCalledWith(
    expect.stringContaining('soxta.example')
  );
});

/** Noto'g'ri paroldа aniq xabar ko'rsatiladi. */
it('Xato parolda xabar chiqadi', async () => {
  const err = new Error('401');
  err.response = { status: 401 };
  signIn.mockRejectedValue(err);

  renderAt('/kirish');
  await submit();

  expect(await screen.findByText('error.credentials')).toBeInTheDocument();
});

/**
 * ⚠️ Tarmoq xatosi «parol noto'g'ri» deb ko'rsatilmasin — odam
 * to'g'ri parolni qayta-qayta kiritishga urinardi.
 */
it('Tarmoq xatosi boshqacha xabar beradi', async () => {
  signIn.mockRejectedValue(new Error('tarmoq'));

  renderAt('/kirish');
  await submit();

  expect(await screen.findByText('error.network')).toBeInTheDocument();
});
