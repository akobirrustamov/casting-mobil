import type { CastingType, Gender } from '@/features/creators/types';

import { isCastingType } from './options';

/**
 * Анкета кандидата: состояние формы, проверка и тело запроса.
 *
 * Логика вынесена из экрана, чтобы её можно было проверить тестом без
 * рендера: правила «что обязательно» и «как превратить ввод в JSON» —
 * ровно то место, где ошибка не видна глазами, а видна только админу,
 * получившему анкету без роста или с датой рождения «2031 год».
 *
 * <h2>Состав полей — как на сайте</h2>
 * `frontend/src/pages/dataForm/DataForm.js`. Отличие одно: поле «Yosh»
 * (возраст) убрано. Новый эндпоинт принимает дату рождения, а возраст
 * считается из неё — два поля, которые могут противоречить друг другу,
 * хуже одного.
 */

/** Значения полей — строки: так их отдаёт `TextInput`, числа собираем при отправке. */
export type ApplicationForm = {
  castingType: CastingType | '';
  gender: Gender | '';
  name: string;
  region: string;
  nationality: string;
  /** Ввод в виде `ДД.ММ.ГГГГ` — так пишут дату в Узбекистане. */
  birthday: string;
  height: string;
  hairColor: string;
  eyeColor: string;
  clothSize: string;
  shoeSize: string;
  bust: string;
  waist: string;
  son: string;
  email: string;
  phone: string;
  telegram: string;
  facebook: string;
  instagram: string;
};

export type FormField = keyof ApplicationForm | 'photos';

export const EMPTY_FORM: ApplicationForm = {
  castingType: '',
  gender: '',
  name: '',
  region: '',
  nationality: '',
  birthday: '',
  height: '',
  hairColor: '',
  eyeColor: '',
  clothSize: '',
  shoeSize: '',
  bust: '',
  waist: '',
  son: '',
  email: '',
  phone: '',
  telegram: '',
  facebook: '',
  instagram: '',
};

/**
 * Обязательные поля — те же, что звёздочкой отмечены на сайте.
 *
 * ⚠️ Бэкенд строже только в части `castingType/gender/name/birthday/phone`,
 * остальное он принимает пустым. Требуем больше, чем сервер, намеренно:
 * это правила анкеты, которые заказчик уже утвердил на сайте, и админ
 * отбирает кандидатов по росту и внешности. Анкета без них бесполезна.
 */
export const REQUIRED_FIELDS: readonly (keyof ApplicationForm)[] = [
  'castingType',
  'gender',
  'name',
  'region',
  'nationality',
  'birthday',
  'height',
  'hairColor',
  'eyeColor',
  'email',
  'phone',
];

/**
 * Фото: сайт требует не меньше шести («6 tadan kam bo'lmasin»), сервер
 * принимает от 1 до 10.
 *
 * ⚠️ Минимум — с сайта, максимум — с сервера. Шесть — это правило
 * заказчика (лицо + рост в полный рост с разных сторон); одиннадцатое
 * фото сервер отклонит, и лучше не давать его выбрать вовсе.
 */
export const MIN_PHOTOS = 6;
export const MAX_PHOTOS = 10;

/** Серверные пределы роста (`height` 50..250). */
export const HEIGHT_MIN = 50;
export const HEIGHT_MAX = 250;

/** Предел размера фото на сервере — `ImageSizeLimit`, 10 МБ. */
export const MAX_PHOTO_BYTES = 10 * 1024 * 1024;

/** Ключ перевода причины — экран сам подставит текст. */
export type FormErrors = Partial<Record<FormField, string>>;

/**
 * Предел длины каждого поля.
 *
 * <h2>Почему не 255 на всё</h2>
 * 255 — это предел колонки в базе, а не разумная длина имени или размера
 * обуви. Поле, где ждут «42», принимало двести пятьдесят символов, и такая
 * строка уезжала админу в список анкет как есть.
 */
export const FIELD_LIMITS: Partial<Record<FormField, number>> = {
  name: 60,
  region: 40,
  nationality: 40,
  hairColor: 30,
  eyeColor: 30,
  height: 3,
  clothSize: 3,
  shoeSize: 3,
  bust: 3,
  waist: 3,
  son: 3,
  email: 100,
  phone: 20,
  telegram: 50,
  facebook: 100,
  instagram: 50,
};

/** Поля, куда пускаем только 0–9: рост и мерки. */
const DIGITS_ONLY: FormField[] = ['height', 'clothSize', 'shoeSize', 'bust', 'waist', 'son'];

/**
 * Очистка введённого значения.
 *
 * ⚠️ Вызывается в ОДНОМ месте — в `set` экрана анкеты. Если раскидать
 * `replace` по каждому полю, новое поле добавят без фильтра и заметят это
 * уже по мусору в заявке.
 *
 * Дата рождения сюда не попадает: у неё своя маска
 * (`formatBirthdayInput`), она ставит точки и сама режет длину.
 */
export function sanitizeField(field: FormField, raw: string): string {
  let value = raw;

  if (DIGITS_ONLY.includes(field)) {
    value = value.replace(/\D/g, '');
  } else if (field === 'phone') {
    // Цифры и ОДИН «+» в начале — как ждёт бэкенд.
    value = value.replace(/[^\d+]/g, '').replace(/(?!^)\+/g, '');
  }

  const limit = FIELD_LIMITS[field];
  return limit ? value.slice(0, limit) : value;
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/**
 * Маска даты: цифры → `ДД.ММ.ГГГГ`.
 *
 * Своя маска, а не нативный календарь: `@react-native-community/datetimepicker`
 * — ещё один нативный модуль и ещё одна пересборка, а выбирать год
 * рождения прокруткой календаря назад на двадцать лет неудобно.
 */
export function formatBirthdayInput(raw: string): string {
  const d = raw.replace(/\D/g, '').slice(0, 8);
  if (d.length <= 2) return d;
  if (d.length <= 4) return `${d.slice(0, 2)}.${d.slice(2)}`;
  return `${d.slice(0, 2)}.${d.slice(2, 4)}.${d.slice(4)}`;
}

/**
 * `ДД.ММ.ГГГГ` → `YYYY-MM-DD`, либо `null`, если даты не существует.
 *
 * ⚠️ Проверяем через обратную сборку `Date`: `new Date(2001, 1, 31)`
 * молча превращается в 3 марта, и без сверки «31.02» ушло бы на сервер
 * как настоящая дата.
 *
 * @param today точка отсчёта — параметром, чтобы тест не зависел от дня запуска
 */
export function parseBirthday(text: string, today: Date = new Date()): string | null {
  const m = /^(\d{2})\.(\d{2})\.(\d{4})$/.exec(text.trim());
  if (!m) return null;

  const day = Number(m[1]);
  const month = Number(m[2]);
  const year = Number(m[3]);

  const date = new Date(year, month - 1, day);
  if (
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day
  ) {
    return null;
  }

  // Не из будущего и не старше ста двадцати лет — остальное опечатка.
  if (date.getTime() > today.getTime() || year < today.getFullYear() - 120) {
    return null;
  }

  return `${m[3]}-${m[2]}-${m[1]}`;
}

/** Телефон — только цифры и ведущий плюс. Пробелы и скобки люди ставят сами. */
export function normalizePhone(raw: string): string {
  const trimmed = raw.trim();
  const digits = trimmed.replace(/\D/g, '');
  return trimmed.startsWith('+') ? `+${digits}` : digits;
}

function isValidPhone(raw: string): boolean {
  return /^\+?\d{9,15}$/.test(normalizePhone(raw));
}

/**
 * Проверка перед отправкой.
 *
 * @param photoCount сколько фото УЖЕ загружено на сервер — не выбрано,
 *        а именно загружено: id есть только у них
 * @returns пустой объект — можно отправлять
 */
export function validateApplication(form: ApplicationForm, photoCount: number, today?: Date): FormErrors {
  const errors: FormErrors = {};

  for (const field of REQUIRED_FIELDS) {
    if (String(form[field]).trim() === '') {
      errors[field] = 'casting.form.errors.required';
    }
  }

  if (form.castingType && !isCastingType(form.castingType)) {
    errors.castingType = 'casting.form.errors.required';
  }

  if (form.birthday.trim() && parseBirthday(form.birthday, today) === null) {
    errors.birthday = 'casting.form.errors.birthday';
  }

  if (form.height.trim()) {
    const h = Number(form.height.trim());
    if (!Number.isInteger(h) || h < HEIGHT_MIN || h > HEIGHT_MAX) {
      errors.height = 'casting.form.errors.height';
    }
  }

  if (form.email.trim() && !EMAIL_RE.test(form.email.trim())) {
    errors.email = 'casting.form.errors.email';
  }

  if (form.phone.trim() && !isValidPhone(form.phone)) {
    errors.phone = 'casting.form.errors.phone';
  }

  if (photoCount < MIN_PHOTOS) {
    errors.photos = 'casting.form.errors.photosMin';
  } else if (photoCount > MAX_PHOTOS) {
    errors.photos = 'casting.form.errors.photosMax';
  }

  return errors;
}

/** Тело `POST /api/v1/app/casting/applications`. */
export type ApplicationPayload = {
  castingType: CastingType;
  gender: Gender;
  name: string;
  region?: string;
  nationality?: string;
  birthday: string;
  height?: number;
  hairColor?: string;
  eyeColor?: string;
  clothSize?: string;
  shoeSize?: string;
  bust?: string;
  waist?: string;
  son?: string;
  email?: string;
  phone: string;
  telegram?: string;
  facebook?: string;
  instagram?: string;
  photos: string[];
};

/** Пустая строка — не значение: в JSON её не шлём вовсе. */
function opt(value: string): string | undefined {
  const v = value.trim();
  return v === '' ? undefined : v;
}

/**
 * Форма → тело запроса. Вызывать ТОЛЬКО после `validateApplication`
 * без ошибок: обязательные поля здесь считаются заполненными.
 *
 * ⚠️ Грудь и бёдра у мужчины не отправляем, даже если их успели ввести:
 * на сайте эти поля для `male` скрыты, и человек, сменивший пол в
 * форме, не видит, что они остались заполнены.
 */
export function buildPayload(form: ApplicationForm, photoIds: string[], today?: Date): ApplicationPayload {
  const isMale = form.gender === 'male';
  const height = form.height.trim() ? Number(form.height.trim()) : undefined;

  return {
    castingType: form.castingType as CastingType,
    gender: form.gender as Gender,
    name: form.name.trim(),
    region: opt(form.region),
    nationality: opt(form.nationality),
    birthday: parseBirthday(form.birthday, today) as string,
    height,
    hairColor: opt(form.hairColor),
    eyeColor: opt(form.eyeColor),
    clothSize: opt(form.clothSize),
    shoeSize: opt(form.shoeSize),
    bust: isMale ? undefined : opt(form.bust),
    waist: opt(form.waist),
    son: isMale ? undefined : opt(form.son),
    email: opt(form.email),
    phone: normalizePhone(form.phone),
    telegram: opt(form.telegram),
    facebook: opt(form.facebook),
    instagram: opt(form.instagram),
    photos: photoIds,
  };
}
