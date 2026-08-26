/**
 * ТЗ §53 — QIDIRUVLI TANLASH.
 *
 * <h2>Nima uchun aynan bu tekshiriladi</h2>
 * `Select` nativ `<select>` ning O'RNINI bosadi va u 30 dan ortiq joyda
 * ishlatiladi. Nativ xatti-harakatdan jimgina chetga chiqsa, xato bir
 * joyda emas — butun panelda paydo bo'ladi. Shuning uchun aynan
 * "nativga o'xshashlik" tekshiriladi:
 *
 * 1. `e.target.value` HAR DOIM satr. `<option value={5}>` raqam
 *    qaytarsa, chaqiruv joylaridagi `Number(e.target.value)` va
 *    `e.target.value === ''` mantig'i buzilardi.
 * 2. Bir xil variant qayta tanlansa `onChange` CHAQIRILMAYDI —
 *    nativ `<select>` ham shunday qiladi.
 * 3. `value` berilmagan `<option>` uchun matnning o'zi qiymat bo'ladi.
 *
 * Qolgan uchtasi qidiruvning o'zi haqida: filtr, klaviatura va
 * qidiruv maydoni QACHON ko'rinishi.
 */
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Select from '../Select';
import SearchableSelect from '../SearchableSelect';
import { PanelI18nProvider } from '../../i18n';

const MANY = ['MOVIE', 'SERIAL', 'CONCERT', 'SHOW', 'DOCUMENTARY', 'CARTOON', 'CLIP', 'THEATRE'];

function wrap(ui) {
  return render(<PanelI18nProvider>{ui}</PanelI18nProvider>);
}

function openList() {
  return userEvent.click(screen.getByRole('combobox'));
}

test('tanlangan variantning YORLIG\'I ko\'rsatiladi, xom qiymati emas', () => {
  wrap(
    <Select value="MOVIE" onChange={() => {}} aria-label="Turi">
      <option value="">Barchasi</option>
      <option value="MOVIE">Kino</option>
      <option value="SERIAL">Serial</option>
    </Select>,
  );
  expect(screen.getByRole('combobox')).toHaveTextContent('Kino');
  expect(screen.getByRole('combobox')).not.toHaveTextContent('MOVIE');
});

test('qiymat HAR DOIM satr bo\'lib uziladi — raqamli `option` bo\'lsa ham', async () => {
  const onChange = jest.fn();
  wrap(
    <Select value="" onChange={onChange} aria-label="Mavsum">
      <option value="">Tanlang</option>
      <option value={5}>Beshinchi</option>
    </Select>,
  );
  await openList();
  await userEvent.click(screen.getByRole('option', { name: 'Beshinchi' }));

  expect(onChange).toHaveBeenCalledTimes(1);
  const value = onChange.mock.calls[0][0].target.value;
  expect(value).toBe('5');
  expect(typeof value).toBe('string');
});

test('bir xil variant qayta tanlansa onChange chaqirilmaydi', async () => {
  const onChange = jest.fn();
  wrap(
    <Select value="MOVIE" onChange={onChange} aria-label="Turi">
      <option value="MOVIE">Kino</option>
      <option value="SERIAL">Serial</option>
    </Select>,
  );
  await openList();
  await userEvent.click(screen.getByRole('option', { name: 'Kino' }));
  expect(onChange).not.toHaveBeenCalled();
});

test('`value` berilmagan option uchun matnning o\'zi qiymat bo\'ladi', async () => {
  const onChange = jest.fn();
  wrap(
    <Select value="" onChange={onChange} aria-label="Turi">
      <option value="">Tanlang</option>
      <option>Hammasi</option>
    </Select>,
  );
  await openList();
  await userEvent.click(screen.getByRole('option', { name: 'Hammasi' }));
  expect(onChange.mock.calls[0][0].target.value).toBe('Hammasi');
});

test('uzun ro\'yxatda qidiruv maydoni bor va ro\'yxatni filtrlaydi', async () => {
  wrap(
    <Select value="" onChange={() => {}} aria-label="Turi">
      {MANY.map((x) => <option key={x} value={x}>{x}</option>)}
    </Select>,
  );
  await openList();

  const list = screen.getByRole('listbox');
  expect(within(list).getAllByRole('option')).toHaveLength(MANY.length);

  await userEvent.type(screen.getByRole('textbox'), 'con');
  const shown = within(screen.getByRole('listbox')).getAllByRole('option');
  expect(shown.map((o) => o.textContent)).toEqual(['CONCERT']);
});

test('qisqa ro\'yxatda qidiruv maydoni KO\'RSATILMAYDI', async () => {
  wrap(
    <Select value="" onChange={() => {}} aria-label="Holat">
      <option value="A">Faol</option>
      <option value="B">Nofaol</option>
    </Select>,
  );
  await openList();
  // Ro'yxat ochiq, lekin qidiradigan narsa yo'q: ikkala variant ko'z oldida.
  expect(screen.getByRole('listbox')).toBeInTheDocument();
  expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
});

test('yopiq ro\'yxat klaviatura bilan ochiladi va Enter tanlaydi', async () => {
  const onChange = jest.fn();
  wrap(
    <Select value="A" onChange={onChange} aria-label="Holat">
      <option value="A">Birinchi</option>
      <option value="B">Ikkinchi</option>
    </Select>,
  );
  screen.getByRole('combobox').focus();
  await userEvent.keyboard('{ArrowDown}');   // ro'yxatni ochadi
  await userEvent.keyboard('{ArrowDown}');   // «Ikkinchi» ga tushadi
  await userEvent.keyboard('{Enter}');

  expect(onChange.mock.calls[0][0].target.value).toBe('B');
  expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
});

test('qidiruvdan keyin fokus TUGMAGA qaytadi', async () => {
  // ⚠️ Bu ataylab UZUN ro'yxat. Qisqa ro'yxatda fokus tugmadan umuman
  // ketmaydi, shuning uchun u yerda bu tekshiruv HECH NARSANI ushlamaydi
  // — mutatsiya sinovi aynan shuni ko'rsatdi.
  const onChange = jest.fn();
  wrap(
    <Select value="MOVIE" onChange={onChange} aria-label="Turi">
      {MANY.map((x) => <option key={x} value={x}>{x}</option>)}
    </Select>,
  );
  await openList();
  // Qidiruv maydoni fokusni O'ZIGA oldi.
  expect(screen.getByRole('textbox')).toHaveFocus();

  await userEvent.keyboard('{ArrowDown}{Enter}');

  expect(onChange.mock.calls[0][0].target.value).toBe('SERIAL');
  // Fokus tugmaga qaytmasa, u `<body>` ga tushib qolardi va keyingi
  // Tab bosilganda sahifa BOSHIDAN boshlanardi.
  expect(screen.getByRole('combobox')).toHaveFocus();
});

test('SearchableSelect `id` ning ASL turini qaytaradi', async () => {
  const onChange = jest.fn();
  wrap(
    <SearchableSelect
      value={null}
      options={[{ id: 7, label: 'Drama' }, { id: 8, label: 'Komediya' }]}
      onChange={onChange}
      emptyLabel="Yo'q"
      ariaLabel="Janr"
    />,
  );
  await openList();
  await userEvent.click(screen.getByRole('option', { name: 'Komediya' }));
  expect(onChange).toHaveBeenCalledWith(8);          // 8 — '8' emas
});

test('SearchableSelect bo\'sh variant uchun null qaytaradi', async () => {
  const onChange = jest.fn();
  wrap(
    <SearchableSelect
      value={7}
      options={[{ id: 7, label: 'Drama' }]}
      onChange={onChange}
      emptyLabel="Yo'q"
      ariaLabel="Janr"
    />,
  );
  await openList();
  await userEvent.click(screen.getByRole('option', { name: "Yo'q" }));
  expect(onChange).toHaveBeenCalledWith(null);
});

const GATED = (
  <>
    <option value="DRAFT">Qoralama</option>
    <option value="PUBLISHED" disabled>Chop etilgan</option>
    <option value="ARCHIVED">Arxivlangan</option>
  </>
);

// ⚠️ Bu ikkitasi shunchaki bezak emas: `EpisodesTab`, `BannerPage` va
// `ContentPage` da «PUBLISHED» aynan shu yo'l bilan `CONTENT_PUBLISH`
// ruxsati yo'q xodimdan yopiladi. (Backend baribir tekshiradi — bu
// yerda faqat UI to'sig'i qulflanadi.)

test("o'chirilgan variant BOSILGANDA tanlanmaydi", async () => {
  const onChange = jest.fn();
  wrap(<Select value="DRAFT" onChange={onChange} aria-label="Holat">{GATED}</Select>);
  await openList();

  await userEvent.click(screen.getByRole('option', { name: 'Chop etilgan' }));
  expect(onChange).not.toHaveBeenCalled();
  expect(screen.getByRole('listbox')).toBeInTheDocument();   // ro'yxat yopilmadi ham
});

test("klaviatura o'chirilgan variantni SAKRAB o'tadi", async () => {
  // ⚠️ Bu yerda sichqoncha bilan BOSILMAYDI: bosish `onMouseEnter`
  // orqali kursorni allaqachon surib yuborardi va sakrash mantig'i
  // umuman ishga tushmasdi — mutatsiya sinovi aynan shuni ko'rsatdi.
  const onChange = jest.fn();
  wrap(<Select value="DRAFT" onChange={onChange} aria-label="Holat">{GATED}</Select>);
  await openList();

  // Kursor «Qoralama» da. Bir marta pastga — «Chop etilgan» ustidan
  // sakrab, to'g'ridan-to'g'ri «Arxivlangan» ga tushishi kerak.
  await userEvent.keyboard('{ArrowDown}{Enter}');
  expect(onChange.mock.calls[0][0].target.value).toBe('ARCHIVED');
});
