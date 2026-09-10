import { usePathname, useSegments } from 'expo-router';
import { useEffect, useState } from 'react';
import { Text, View } from 'react-native';

import { useAuthStore } from '@/features/auth/store';
import { useDeviceStore } from '@/features/devices/store';

/**
 * ВРЕМЕННАЯ диагностика чёрного экрана (10.09.2026).
 *
 * <h2>Зачем понадобилась</h2>
 * После входа экран становится чёрным, при этом `ErrorBoundary` МОЛЧИТ.
 * Значит это не ошибка рендера: либо приложение ушло на маршрут, который
 * ничего не рисует, либо экран смонтирован, но невидим. По чёрному
 * прямоугольнику эти случаи неразличимы, а `adb logcat` у тестировщика нет.
 *
 * Полоса сверху отвечает ровно на этот вопрос: какой маршрут сейчас
 * смонтирован и что со состоянием входа.
 *
 * ⚠️ УДАЛИТЬ, как только причина найдена. Это отладочный инструмент, а не
 * часть интерфейса: он закрывает собой верх экрана и показывает
 * пользователю внутренности приложения.
 */
const DEBUG = true;

/**
 * Последняя фатальная ошибка JS.
 *
 * ⚠️ Именно `ErrorUtils`, а не `ErrorBoundary`: тот ловит только падения
 * РЕНДЕРА. Ошибка в обработчике нажатия, в `then` или в таймере проходит
 * мимо него — в dev это красный экран, а в release приложение просто
 * перестаёт рисовать. Ровно то, что мы и наблюдаем.
 */
let lastFatal: string | null = null;

function installGlobalHandler(onError: (text: string) => void): void {
  const utils = (
    globalThis as {
      ErrorUtils?: {
        getGlobalHandler: () => (error: unknown, isFatal?: boolean) => void;
        setGlobalHandler: (handler: (error: unknown, isFatal?: boolean) => void) => void;
      };
    }
  ).ErrorUtils;

  if (!utils) return;

  const previous = utils.getGlobalHandler();

  utils.setGlobalHandler((error, isFatal) => {
    const message =
      error instanceof Error ? `${error.name}: ${error.message}` : String(error);
    lastFatal = `${isFatal ? 'FATAL' : 'ERROR'} ${message}`;
    onError(lastFatal);

    // ⚠️ Родной обработчик вызывается ВСЁ РАВНО: подменив его молча, мы
    // отняли бы у приложения его собственное поведение при падении.
    previous(error, isFatal);
  });
}

export function DebugOverlay() {
  const pathname = usePathname();
  const segments = useSegments();
  const isAuthorized = useAuthStore((s) => s.isAuthorized);
  const hasToken = useAuthStore((s) => Boolean(s.token));
  const deviceStatus = useDeviceStore((s) => s.status);

  const [fatal, setFatal] = useState<string | null>(lastFatal);

  useEffect(() => {
    installGlobalHandler(setFatal);
  }, []);

  // ⚠️ Пока всё в порядке — полосы НЕТ.
  //
  // Сначала она висела всегда и показывала маршрут: это отвечало на вопрос
  // «чёрный экран — это пустой маршрут или невидимый экран». Но такая
  // полоса закрывает верх экрана и показывает человеку внутренности
  // приложения, поэтому в собираемой APK она появляется только по факту
  // падения. Если чёрный экран повторится БЕЗ неё — значит фатальной
  // ошибки нет вовсе, и маршрут надо смотреть отдельно, одним `eas update`.
  /**
   * ⚠️ Пока всё в порядке — полосы НЕТ.
   *
   * Она была видна всегда, пока искали чёрный экран после входа: по
   * самому экрану нельзя было понять, какой маршрут смонтирован.
   * Причина найдена (два `router.replace` в один кадр, см.
   * `features/auth/store`), и постоянная полоса больше не нужна — она
   * закрывает верх экрана и показывает человеку внутренности.
   *
   * Остаётся ловушка на фатальные ошибки: если приложение снова
   * замолчит, экран хотя бы скажет, что именно упало.
   */
  if (!DEBUG || fatal === null) return null;

  return (
    <View
      pointerEvents="none"
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        zIndex: 9999,
        backgroundColor: '#000000E6',
        paddingTop: 44,
        paddingBottom: 6,
        paddingHorizontal: 8,
      }}
    >
      <Text style={{ color: '#7CFC00', fontSize: 11 }}>
        path: {pathname || "(bo'sh)"}
      </Text>
      <Text style={{ color: '#7CFC00', fontSize: 11 }}>
        segments: [{segments.join(' / ')}]
      </Text>
      <Text style={{ color: '#7CFC00', fontSize: 11 }}>
        auth: {isAuthorized ? 'ha' : "yo'q"} · token: {hasToken ? 'bor' : "yo'q"} ·
        device: {deviceStatus}
      </Text>
      {fatal ? (
        <Text style={{ color: '#F87171', fontSize: 11 }} numberOfLines={4}>
          {fatal}
        </Text>
      ) : null}
    </View>
  );
}
