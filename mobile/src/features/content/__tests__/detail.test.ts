/**
 * Разбор ответа `GET /api/v1/app/content/{id}` и `.../donors`.
 *
 * <h2>⚠️ Почему это нужно тестами, а не глазами</h2>
 * Все три ошибки, которые здесь ловятся, выглядят на экране ОДИНАКОВО —
 * как «у этого фильма ничего нет»:
 *
 * <ul>
 *   <li>сервер старой сборки отдал на этот адрес `index.html` со
 *       статусом 200 — и карточка «разобралась» в пустоту;</li>
 *   <li>счётчик `0` от сервера превратился в «сервер не сказал» — и
 *       вместо честного нуля на плитке встал прочерк;</li>
 *   <li>рейтинг донатов пришёл без места (`rank`) — и строки
 *       перенумеровались бы по порядку в массиве.</li>
 * </ul>
 *
 * Ни одна из них не роняет экран и не пишет ошибку в консоль.
 */

jest.mock('@/lib/api', () => ({ api: {} }));
jest.mock('@/features/home/api', () => ({ useFeedLanguage: jest.fn() }));
jest.mock('@/features/watch/api', () => ({ useViewerKey: jest.fn() }));

import { ContentDetailUnavailableError, mapDetail, mapDonors } from '../detail';

/** Минимальный ответ, который сервер обязан прислать. */
function card(over: Record<string, unknown> = {}) {
  return { id: 7, title: 'Qalbing egasi', ...over };
}

describe('mapDetail', () => {
  it('HTML-заглушка старого сервера — это ошибка, а не пустая карточка', () => {
    expect(() => mapDetail('<!doctype html><html></html>')).toThrow(
      ContentDetailUnavailableError
    );
    expect(() => mapDetail(null)).toThrow(ContentDetailUnavailableError);
    // Объект без `id` — тоже не карточка: открыть по ней нечего.
    expect(() => mapDetail({ title: 'Film' })).toThrow(ContentDetailUnavailableError);
  });

  it('ноль от сервера остаётся нулём, а отсутствие поля — «не сказал»', () => {
    const zero = mapDetail(card({ viewCount: 0, likeCount: 0, commentCount: 0 }));

    expect(zero.viewCount).toBe(0);
    expect(zero.likeCount).toBe(0);
    expect(zero.commentCount).toBe(0);

    const silent = mapDetail(card());

    expect(silent.viewCount).toBeNull();
    expect(silent.likeCount).toBeNull();
    expect(silent.commentCount).toBeNull();
  });

  it('списки приходят массивами даже когда сервер их не прислал', () => {
    const dto = mapDetail(card());

    expect(dto.genres).toEqual([]);
    expect(dto.cast).toEqual([]);
    expect(dto.galleryMediaIds).toEqual([]);
  });

  it('чужой мусор в списках отбрасывается, а не ломает экран', () => {
    const dto = mapDetail(
      card({ genres: ['Drama', '', null, 42], galleryMediaIds: [1, 'x', null, 3] })
    );

    expect(dto.genres).toEqual(['Drama']);
    expect(dto.galleryMediaIds).toEqual([1, 3]);
  });

  it('актёр приходит с именем персонажа — на макете оно под именем', () => {
    const dto = mapDetail(
      card({
        cast: [
          {
            creatorId: 3,
            name: 'Zarina Yoqubova',
            profession: 'ACTRESS',
            characterName: 'Dilnoza',
          },
        ],
      })
    );

    expect(dto.cast).toHaveLength(1);
    expect(dto.cast[0].characterName).toBe('Dilnoza');
    expect(dto.cast[0].profession).toBe('ACTRESS');
  });

  it('«нравится» у гостя — false, а не undefined', () => {
    expect(mapDetail(card()).liked).toBe(false);
    expect(mapDetail(card({ liked: true })).liked).toBe(true);
  });
});

describe('mapDonors', () => {
  it('пустой список законен, отсутствие массива — нет', () => {
    expect(mapDonors({ donors: [], starsReceived: 0 }).donors).toEqual([]);

    expect(() => mapDonors({ starsReceived: 0 })).toThrow(ContentDetailUnavailableError);
    expect(() => mapDonors('<!doctype html>')).toThrow(ContentDetailUnavailableError);
  });

  it('место берётся у сервера, а не из порядка в массиве', () => {
    const list = mapDonors({
      starsReceived: 1000,
      donors: [
        { rank: 4, name: 'Abdulaziz', stars: 900 },
        { rank: 5, name: 'Malika', stars: 100 },
      ],
    });

    expect(list.donors.map((d) => d.rank)).toEqual([4, 5]);
  });

  it('без места строки нумеруются по порядку — список не остаётся без номеров', () => {
    const list = mapDonors({ donors: [{ name: 'A' }, { name: 'B' }] });

    expect(list.donors.map((d) => d.rank)).toEqual([1, 2]);
    expect(list.donors[0].stars).toBe(0);
  });

  it('безымянный донатчик остаётся в рейтинге — звёзды он отправил', () => {
    const [donor] = mapDonors({ donors: [{ rank: 1, name: null, stars: 50 }] }).donors;

    expect(donor.name).toBeNull();
    expect(donor.stars).toBe(50);
  });
});
