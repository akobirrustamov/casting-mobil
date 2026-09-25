import { router, type Href } from 'expo-router';

/**
 * Сколько миллисекунд после перехода новые нажатия игнорируются.
 *
 * Анимация перехода в стеке — около 350 мс. За это время старая страница
 * ещё на экране и её кнопки нажимаются; с запасом — чтобы «дребезг»
 * пальца и нетерпеливое второе нажатие не проходили.
 */
export const PUSH_COOLDOWN_MS = 700;

/**
 * Сколько игнорируется повтор ТОГО ЖЕ адреса.
 *
 * ⚠️ 25.09.2026: быстрые нажатия на карточку во вкладке «Casting»
 * открывали 3–4 одинаковых профиля. Сетка с каруселями тяжёлая, и на
 * слабом Android новый экран появляется позже 700 мс — старая карточка
 * всё ещё под пальцем, и следующее нажатие проходило. На тот же адрес
 * замок держится дольше.
 */
export const SAME_HREF_COOLDOWN_MS = 2500;

let lastPushAt = -Infinity;
let lastHref: string | null = null;

/**
 * `router.push`, который не открывает одну страницу десять раз.
 *
 * <h2>⚠️ Что чинилось (10.09.2026)</h2>
 * Десять быстрых нажатий на «Izohlar» открывали десять одинаковых
 * страниц — «назад» приходилось жать десять раз. То же было у плиток
 * «Yulduzlar» и «Uzcasting»: пока идёт анимация перехода, старая
 * страница ещё на экране и каждое нажатие честно делает новый `push`.
 *
 * <h2>Почему не `dangerouslySingular` из expo-router</h2>
 * Он убирает из истории ПРЕЖНИЙ экран того же маршрута, но переход всё
 * равно выполняется каждый раз: десять нажатий — десять анимаций подряд.
 * Здесь лишние нажатия не доходят до навигации вовсе.
 *
 * ⚠️ Замок ОБЩИЙ, а не на адрес: «Izohlar» и тут же «Yulduzlar» — это
 * тоже две страницы друг на друге, а человек хотел одну.
 */
export function pushOnce(href: Href, now: number = Date.now()): boolean {
  const key = typeof href === 'string' ? href : JSON.stringify(href);
  if (now - lastPushAt < PUSH_COOLDOWN_MS) return false;
  if (key === lastHref && now - lastPushAt < SAME_HREF_COOLDOWN_MS) return false;
  lastPushAt = now;
  lastHref = key;
  router.push(href);
  return true;
}

/** ⚠️ Только для тестов: замок живёт на уровне модуля. */
export function resetPushOnceForTests() {
  lastPushAt = -Infinity;
  lastHref = null;
}
