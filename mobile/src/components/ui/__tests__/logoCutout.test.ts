import fs from 'fs';
import path from 'path';

import { CUTOUT, CUTOUT_VIEW, LOGO_PAD } from '../logoCutout';
import {
  LOGO_GRADIENT,
  LOGO_PATHS,
  LOGO_PLAY,
  LOGO_SPROCKETS,
  LOGO_VIEWBOX,
} from '../logoPaths';

/**
 * `BrandLoader` держится на двух вещах, и обе легко сломать молча:
 *
 *   1. `logoPaths.ts` СГЕНЕРИРОВАН из `assets/brand/logo.svg`. Новый
 *      логотип — и загрузчик покажет старый знак, а вырез разъедется
 *      с картинкой в `Logo.tsx`. Ничего не упадёт, просто станет криво.
 *   2. Вырез работает ТОЛЬКО с `evenodd` и ТОЛЬКО с рамкой снаружи.
 *      Уберут рамку — знак пропадёт, поменяют правило заливки — холст
 *      закрасится целиком.
 *
 * Поэтому тест сверяет файл с самим SVG и проверяет чётность выреза в
 * точках, каждая из которых отвечает за свою деталь знака.
 */

const SVG = fs.readFileSync(
  path.join(__dirname, '../../../../assets/brand/logo.svg'),
  'utf8'
);

describe('logoPaths — слепок assets/brand/logo.svg', () => {
  it('содержит те же контуры, что и SVG', () => {
    const fromSvg = [...SVG.matchAll(/ d="([^"]+)"/g)].map((m) => m[1]);

    expect(fromSvg.length).toBeGreaterThan(0);
    expect(LOGO_PATHS).toEqual(fromSvg);
  });

  it('содержит тот же градиент, что и SVG', () => {
    const stops = [...SVG.matchAll(/stop-color="(#[0-9A-Fa-f]{6})"/g)].map((m) => m[1]);

    expect(LOGO_GRADIENT).toEqual(stops);
  });

  it('описывает знак в его собственных координатах', () => {
    expect(SVG).toContain(`viewBox="0 0 ${LOGO_VIEWBOX} ${LOGO_VIEWBOX}"`);
  });
});

describe('детали знака, по которым идёт анимация', () => {
  it('нашлись все 11 отверстий перфорации', () => {
    expect(LOGO_SPROCKETS).toHaveLength(11);
  });

  it('перфорация перечислена по ходу ленты: сверху вниз, справа налево', () => {
    // Порядок — это и есть анимация: огонёк идёт по индексу массива.
    // Перемешать его значит заставить плёнку мерцать вразнобой, и на
    // глаз причину такой поломки не найти.
    const rows = LOGO_SPROCKETS.map((s) => Math.round(s.y / 10));
    expect(rows).toEqual([...rows].sort((a, b) => a - b));

    for (let i = 1; i < LOGO_SPROCKETS.length; i++) {
      const prev = LOGO_SPROCKETS[i - 1];
      const hole = LOGO_SPROCKETS[i];
      if (rows[i] === rows[i - 1]) expect(hole.x).toBeLessThan(prev.x);
    }
  });

  it('рамка треугольника плея лежит в левой половине знака', () => {
    // Подсветка ставится по этой рамке. Уехавшая рамка означала бы, что
    // «кнопка» пульсирует не там, где нарисована.
    expect(LOGO_PLAY.x + LOGO_PLAY.w).toBeLessThan(LOGO_VIEWBOX / 2);
    expect(LOGO_PLAY.w).toBeGreaterThan(50);
    expect(LOGO_PLAY.h).toBeGreaterThan(50);
  });
});

describe('вырез: знак — дырка, всё остальное — подложка', () => {
  it('начинается рамкой, и рамка выходит за холст', () => {
    // Без рамки чётность не переворачивается и рисоваться будет сам знак,
    // а не окно в нём. А без ЗАПАСА по краю остаётся волосяная щель от
    // сглаживания, сквозь которую видно блик.
    const frameStart = Number(CUTOUT.slice(1, CUTOUT.indexOf(',')));

    expect(frameStart).toBeLessThan(-LOGO_PAD);
    expect(CUTOUT_VIEW).toBe(LOGO_VIEWBOX + LOGO_PAD * 2);
  });

  /**
   * Контрольные точки. `true` — закрашено подложкой, `false` — дырка,
   * сквозь которую видно движущиеся слои.
   */
  const PROBES: [string, [number, number], boolean][] = [
    ['поле вокруг знака', [-50, -50], true],
    ['угол холста', [590, 590], true],
    // Самая кромка: сюда доставала щель, из-за которой по краю знака
    // проступала светлая рамка.
    ['кромка холста', [LOGO_VIEWBOX + LOGO_PAD - 1, LOGO_VIEWBOX + LOGO_PAD - 1], true],
    ['левая стойка ленты', [30, 300], false],
    ['тёмный зазор перед плеем', [100, 255], true],
    ['треугольник плея', [170, 255], false],
    ['верхняя плёнка между отверстиями', [466.7, 366], false],
    ['верхняя плёнка, отверстие', [481.7, 366], true],
    ['нижняя плёнка между отверстиями', [315, 434], false],
    ['нижняя плёнка, отверстие', [300.5, 434], true],
  ];

  it.each(PROBES)('%s', (_name, point, filled) => {
    expect(isFilled(CUTOUT, point)).toBe(filled);
  });

  it('без рамки знак рисуется вместо окна', () => {
    // Обратная проверка: тот же вырез без первой подфигуры даёт ровно
    // противоположную картинку. Она подтверждает, что предыдущие девять
    // ожиданий держатся именно на рамке, а не совпали случайно.
    const withoutFrame = LOGO_PATHS.join(' ');

    for (const [, point, filled] of PROBES) {
      expect(isFilled(withoutFrame, point)).toBe(!filled);
    }
  });
});

/**
 * Закрашена ли точка при заливке `evenodd`.
 *
 * Считаем пересечения луча со всеми подфигурами — так же, как это делает
 * растеризатор. Все контуры знака состоят только из `M`/`L`/`Z`, поэтому
 * достаточно разобрать пары координат: кривых там нет.
 */
function isFilled(d: string, [x, y]: [number, number]): boolean {
  const crossings = d
    .split('Z')
    .map((sub) => [...sub.matchAll(/(-?[\d.]+),(-?[\d.]+)/g)].map((m) => [+m[1], +m[2]]))
    .filter((points) => points.length >= 3)
    .filter((points) => crossesRay(points, x, y)).length;

  return crossings % 2 === 1;
}

function crossesRay(points: number[][], x: number, y: number): boolean {
  let inside = false;

  for (let i = 0, j = points.length - 1; i < points.length; j = i++) {
    const [xi, yi] = points[i];
    const [xj, yj] = points[j];

    if (yi > y !== yj > y && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi) {
      inside = !inside;
    }
  }

  return inside;
}
