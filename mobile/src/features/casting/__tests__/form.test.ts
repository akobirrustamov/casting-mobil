/**
 * Анкета кандидата — проверка и сборка тела запроса.
 *
 * <h2>⚠️ Почему это нужно тестами</h2>
 * Ошибка здесь не видна кандидату: форма «отправилась», а админ получил
 * анкету с «31.02» вместо даты или с грудью/бёдрами у мужчины, которые
 * тот не видел на экране. Заметить это можно только на данных.
 */
import {
  EMPTY_FORM,
  MIN_PHOTOS,
  buildPayload,
  formatBirthdayInput,
  parseBirthday,
  validateApplication,
  type ApplicationForm,
} from '../form';

const TODAY = new Date(2026, 8, 19);

const FILLED: ApplicationForm = {
  ...EMPTY_FORM,
  castingType: 'model',
  gender: 'female',
  name: '  Madina Karimova ',
  region: 'Toshkent',
  nationality: "O'zbek",
  birthday: '05.03.2001',
  height: '172',
  hairColor: 'Qora',
  eyeColor: "Jigarrang",
  email: 'madina@example.com',
  phone: '+998 90 123-45-67',
  bust: '86',
  son: '92',
};

describe('formatBirthdayInput', () => {
  it.each([
    ['0', '0'],
    ['05', '05'],
    ['0503', '05.03'],
    ['05032001', '05.03.2001'],
    ['05.03.2001xx9', '05.03.2001'],
  ])('%s → %s', (raw, expected) => {
    expect(formatBirthdayInput(raw)).toBe(expected);
  });
});

describe('parseBirthday', () => {
  it('настоящая дата → YYYY-MM-DD', () => {
    expect(parseBirthday('05.03.2001', TODAY)).toBe('2001-03-05');
  });

  /** `new Date(2001, 1, 31)` молча становится 3 марта — такая дата не должна пройти. */
  it('несуществующее число отклоняется', () => {
    expect(parseBirthday('31.02.2001', TODAY)).toBeNull();
  });

  it('дата из будущего отклоняется', () => {
    expect(parseBirthday('01.01.2030', TODAY)).toBeNull();
  });

  it('неполный ввод отклоняется', () => {
    expect(parseBirthday('05.03.20', TODAY)).toBeNull();
  });
});

describe('validateApplication', () => {
  it('заполненная анкета с шестью фото проходит', () => {
    expect(validateApplication(FILLED, MIN_PHOTOS, TODAY)).toEqual({});
  });

  it('пустая анкета называет каждое обязательное поле', () => {
    const errors = validateApplication(EMPTY_FORM, 0, TODAY);
    for (const field of [
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
    ]) {
      expect(errors).toHaveProperty(field, 'casting.form.errors.required');
    }
    expect(errors.photos).toBe('casting.form.errors.photosMin');
  });

  /** Необязательные поля пустыми — не ошибка. */
  it('необязательные поля могут быть пустыми', () => {
    const errors = validateApplication({ ...FILLED, clothSize: '', telegram: '', bust: '' }, MIN_PHOTOS, TODAY);
    expect(errors).toEqual({});
  });

  it.each([
    ['49', 'casting.form.errors.height'],
    ['251', 'casting.form.errors.height'],
    ['1.7', 'casting.form.errors.height'],
  ])('рост %s вне пределов сервера', (height, key) => {
    expect(validateApplication({ ...FILLED, height }, MIN_PHOTOS, TODAY).height).toBe(key);
  });

  it('неверные почта, телефон и дата', () => {
    const errors = validateApplication(
      { ...FILLED, email: 'madina@', phone: '12-34', birthday: '31.02.2001' },
      MIN_PHOTOS,
      TODAY,
    );
    expect(errors.email).toBe('casting.form.errors.email');
    expect(errors.phone).toBe('casting.form.errors.phone');
    expect(errors.birthday).toBe('casting.form.errors.birthday');
  });

  it('фото: меньше шести и больше десяти — ошибки', () => {
    expect(validateApplication(FILLED, MIN_PHOTOS - 1, TODAY).photos).toBe('casting.form.errors.photosMin');
    expect(validateApplication(FILLED, 11, TODAY).photos).toBe('casting.form.errors.photosMax');
  });
});

describe('buildPayload', () => {
  const ids = ['a', 'b', 'c', 'd', 'e', 'f'];

  it('чистит ввод и собирает контракт бэкенда', () => {
    const payload = buildPayload(FILLED, ids, TODAY);
    expect(payload).toMatchObject({
      castingType: 'model',
      gender: 'female',
      name: 'Madina Karimova',
      birthday: '2001-03-05',
      height: 172,
      phone: '+998901234567',
      bust: '86',
      son: '92',
      photos: ids,
    });
  });

  /** Пустое необязательное поле не уходит пустой строкой. */
  it('пустые необязательные поля не отправляются', () => {
    const payload = buildPayload({ ...FILLED, telegram: '  ', clothSize: '' }, ids, TODAY);
    expect(payload.clothSize).toBeUndefined();
    expect(JSON.parse(JSON.stringify(payload))).not.toHaveProperty('telegram');
  });

  /** На сайте эти поля у мужчин скрыты — введённое до смены пола не должно утечь. */
  it('у мужчины грудь и бёдра не отправляются', () => {
    const payload = buildPayload({ ...FILLED, gender: 'male' }, ids, TODAY);
    expect(payload.bust).toBeUndefined();
    expect(payload.son).toBeUndefined();
  });
});
