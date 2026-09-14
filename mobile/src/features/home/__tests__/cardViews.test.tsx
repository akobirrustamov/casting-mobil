/**
 * Просмотры на карточке ленты.
 *
 * <h2>Что здесь ломается тихо</h2>
 * 1. Ноль. На экране контента он показывается — это честный факт про
 *    один фильм. Под КАЖДОЙ карточкой ленты тот же ноль читается как
 *    «этого никто не смотрит», хотя контент просто вчерашний. Правила
 *    разные намеренно, и уравнять их — однострочная правка, которую
 *    никто не заметит на скриншоте.
 * 2. `null`. Старая сборка бэкенда поля не отдаёт вовсе; превратить его
 *    в «0» значило бы выдумать цифру разом на всей ленте.
 *
 * Здесь же проверяется, что рядом со счётчиком НЕ появилась длительность:
 * заказчик убрал её со всей витрины 14.09.2026, а данные для неё в фиде
 * остались.
 */

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('expo-router', () => ({ router: { push: jest.fn() } }));
jest.mock('expo-image', () => ({ Image: 'Image' }));

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

jest.mock('@/lib/api', () => ({ mediaUrl: () => undefined }));

import { act, create, type ReactTestRenderer } from 'react-test-renderer';
import { Text } from 'react-native';

import { ContentPoster } from '../sections';

import type { ContentCard } from '../types';

function card(over: Partial<ContentCard> = {}): ContentCard {
  return {
    id: 7,
    slug: 'film',
    title: 'Film',
    shortDescription: null,
    contentType: 'MOVIE',
    orientation: 'LANDSCAPE',
    accessPolicy: 'FREE',
    ageRating: null,
    posterMediaId: null,
    durationSeconds: null,
    episodeCount: null,
    genre: null,
    viewCount: null,
    likeCount: null,
    ...over,
  };
}

function texts(data: ContentCard): string[] {
  let tree!: ReactTestRenderer;
  act(() => {
    tree = create(<ContentPoster card={data} width={105} />);
  });
  return tree.root
    .findAllByType(Text)
    .map((node) => String(node.props.children))
    .filter(Boolean);
}

/**
 * Всё, кроме названия и метки доступа: их карточка рисует всегда.
 *
 * ⚠️ Метка сравнивается БЕЗ учёта регистра: бейдж переводит подпись в
 * прописные сам (`components/ui/Badge`, `toUpperCase()` вместо
 * `textTransform`), и ключ `common.free` приезжает сюда как
 * «COMMON.FREE».
 */
function extras(data: ContentCard): string[] {
  return texts(data).filter(
    (s) => s !== 'Film' && !s.toLowerCase().startsWith('common.')
  );
}

describe('счётчик на карточке', () => {
  it('сервер не прислал поля — счётчика нет', () => {
    expect(extras(card({ viewCount: null }))).toEqual([]);
  });

  it('ноль не рисуется: под каждой новинкой он читался бы как приговор', () => {
    // ⚠️ На экране контента ноль КАК РАЗ показывается
    // (`watch/__tests__/statChips`). Разница намеренная, и без этой
    // пары тестов её сотрут одной строкой.
    expect(extras(card({ viewCount: 0 }))).toEqual([]);
  });

  it('длинное число сокращается — на обложке нет места', () => {
    // ⚠️ Заказчик 14.09.2026: «view larni uzun bo'lib ketsa 1.2k 1.5m
    // qilib ber». Правило целиком — в `lib/compactCount`; здесь важно,
    // что карточка берёт именно его, а не разбиение по разрядам.
    expect(extras(card({ viewCount: 12345 }))).toEqual(['12k']);
    expect(extras(card({ viewCount: 5606 }))).toEqual(['5.6k']);
  });

  it('короткое число остаётся точным', () => {
    expect(extras(card({ viewCount: 340 }))).toEqual(['340']);
  });

  it('длительности на карточке нет — её убрал заказчик', () => {
    // ⚠️ 14.09.2026: «video davomiyligi qiymati ko'rsatish olib tashlash
    // kerak, kerak emas». Поле `durationSeconds` фид отдаёт по-прежнему —
    // значит вернуть таймкод на кадр можно одной строкой, незаметно для
    // скриншота. На экране контента длительность как раз ОСТАЁТСЯ.
    const shown = texts(card({ viewCount: 340, durationSeconds: 5400 }));

    expect(shown).toContain('340');
    expect(shown).not.toContain('01:30:00');
  });
});
