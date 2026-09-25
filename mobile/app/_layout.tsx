import '@/i18n';
import '../global.css';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
// SDK 56+: react-navigation импортируется только через expo-router.
// Прямой @react-navigation/native ломает бандл.
import { useFonts } from 'expo-font';
import { Stack, router, useRootNavigationState, usePathname } from 'expo-router';
import { ThemeProvider } from 'expo-router/react-navigation';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { SplashOverlay } from '@/components/SplashOverlay';
import { DebugOverlay } from '@/components/states/DebugOverlay';
import { OfflineBanner } from '@/components/states/OfflineBanner';
import { useAuthStore } from '@/features/auth/store';
import { useAppConfigSync } from '@/features/config/api';
import { useDeviceStore } from '@/features/devices/store';
import { useFavoritesStore } from '@/features/favorites/store';
import { useMutedAuthors } from '@/features/comments/mutedAuthors';
import { usePushNotifications } from '@/features/notifications/push';
import { isOnboardingSeen } from '@/features/onboarding/store';
import i18nInstance from '@/i18n';
import { loadLanguage } from '@/i18n/storage';
import { colors, navigationTheme } from '@/theme/tokens';
import { FONT_ASSETS } from '@/theme/typography';

/**
 * ⚠️ Экран ошибки вместо чёрного прямоугольника.
 *
 * expo-router берёт этот экспорт из файла маршрута и показывает его
 * вместо упавшего поддерева. Стоит в КОРНЕВОЙ раскладке, поэтому
 * накрывает всё приложение разом — иначе пришлось бы помнить про него
 * в каждом новом `_layout`, а забытый файл снова давал бы чёрный экран.
 *
 * ⚠️ Это диагностика, а не «починка»: боевую ошибку она не убирает, но
 * превращает молчаливый чёрный экран в текст, который тестировщик может
 * переслать. До этого единственным способом узнать причину был
 * `adb logcat`.
 */
export { AppErrorBoundary as ErrorBoundary } from '@/components/states/AppErrorBoundary';

/** `useAppConfigSync` нужен клиент запросов — поэтому компонент ВНУТРИ провайдера. */
function AppConfigSync() {
  useAppConfigSync();
  return null;
}

/** По подписи к макету splash висит 1–2 секунды. */
const SPLASH_MIN_MS = 1300;

export default function RootLayout() {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: 2,
            staleTime: 30_000,
          },
        },
      })
  );

  const { showSplash, fontsReady } = useBootstrap();
  useDeviceGuard(showSplash);
  usePushNotifications(!showSplash);

  return (
    <GestureHandlerRootView style={{ flex: 1, backgroundColor: colors.ink }}>
      <SafeAreaProvider>
        <QueryClientProvider client={queryClient}>
          <ThemeProvider value={navigationTheme}>
            {/* ТЗ: dark mode первичен, светлой темы нет */}
            <StatusBar style="light" />
            {/*
              ⚠️ Навигатор монтируется ТОЛЬКО после шрифтов.

              Разбор 15.09.2026 по скриншоту с телефона: в таб-баре
              «Med…» и «Casti…» обрезаны, а более длинное «Bosh sahifa» —
              целое; в шапке главной «Premiu». Места хватало с запасом.

              Раньше экраны рисовались ПОД splash сразу, а ждал шрифтов
              только сам splash. Подписи успевали измериться, пока файла
              Manrope ещё не было, — системным шрифтом, он уже. Имя
              семейства после загрузки не меняется, и Fabric не
              перемеряет текст: рисует Manrope в старую ширину. Целыми
              оставались ровно те подписи, у которых позже сменилось
              начертание (активная вкладка — semibold).

              Пока шрифтов нет, сверху всё равно splash — пустота под ним
              не видна.
            */}
            {fontsReady ? (
              <Stack
                screenOptions={{
                  headerShown: false,
                  contentStyle: { backgroundColor: colors.ink },
                }}
              >
                <Stack.Screen name="(tabs)" />
                <Stack.Screen name="onboarding" />
                <Stack.Screen name="(auth)" />
                <Stack.Screen name="devices" />
              </Stack>
            ) : null}

            {/* Один на всё приложение: держит свежей настройку платежей. */}
            <AppConfigSync />

            {/* Поверх навигатора, но под splash — на splash сеть ещё не нужна */}
            <OfflineBanner />

            {showSplash ? <SplashOverlay /> : null}

            {/* ⚠️ ВРЕМЕННО: полоса диагностики чёрного экрана. Удалить
                вместе с `DebugOverlay`, как только причина найдена. */}
            <DebugOverlay />
          </ThemeProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

/**
 * Первый вход: splash → презентация → вход. Со второго запуска презентации нет,
 * а с живым токеном сразу открывается Home.
 *
 * Splash — оверлей поверх навигатора, а не отдельный маршрут: «/» уже занят
 * под (tabs)/index, и отдельным экраном он остался бы в истории переходов.
 *
 * @returns `showSplash` — показывать ли splash; `fontsReady` — можно ли
 *          монтировать навигатор (см. разбор над `<Stack>`)
 */
function useBootstrap(): { showSplash: boolean; fontsReady: boolean } {
  const restore = useAuthStore((s) => s.restore);
  const restoreFavorites = useFavoritesStore((s) => s.restore);
  const navigationState = useRootNavigationState();
  const isNavigatorReady = Boolean(navigationState?.key);

  /**
   * Фирменный шрифт (заказчик, 14.09.2026) — читается с диска, а не из
   * системы, поэтому первый кадр его ждёт.
   *
   * ⚠️ Ошибка чтения НЕ держит экран. Не прочитавшийся шрифт — это
   * системное начертание вместо Manrope; вечный splash из-за него был бы
   * несоизмеримо хуже. `useFonts` возвращает ошибку вторым значением,
   * поэтому мы её именно учитываем, а не игнорируем.
   */
  const [fontsLoaded, fontsError] = useFonts(FONT_ASSETS);
  const fontsReady = fontsLoaded || fontsError !== null;

  const [target, setTarget] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      const startedAt = Date.now();
      const [seen, language] = await Promise.all([
        isOnboardingSeen(),
        loadLanguage(),
        restore(),
        restoreFavorites(),
        // Скрытые авторы комментариев: поднимаем до первого экрана,
        // иначе их записи успели бы мелькнуть в ленте.
        useMutedAuthors.getState().restore(),
      ]);
      const { isAuthorized } = useAuthStore.getState();

      // Язык применяем до снятия splash — иначе первый экран мигнёт узбекским
      if (language !== i18nInstance.language) {
        await i18nInstance.changeLanguage(language);
      }

      // Даже если всё прочиталось мгновенно, держим splash положенное время —
      // иначе он мелькает на долю секунды и выглядит как сбой.
      const remaining = SPLASH_MIN_MS - (Date.now() - startedAt);
      if (remaining > 0) {
        await new Promise((resolve) => setTimeout(resolve, remaining));
      }
      if (cancelled) return;

      if (!seen) {
        setTarget('/onboarding');
      } else if (isAuthorized) {
        setTarget('/(tabs)');
      } else {
        setTarget('/(auth)/sign-in');
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [restore, restoreFavorites]);

  useEffect(() => {
    if (!target || !isNavigatorReady || !fontsReady || done) return;

    // '/(tabs)' — стартовый маршрут, переходить никуда не нужно
    if (target !== '/(tabs)') {
      router.replace(target);
    }

    // Снимаем оверлей следующим кадром, когда новый экран уже отрисован,
    // иначе на мгновение мелькает то, что было под ним.
    const frame = requestAnimationFrame(() => setDone(true));
    return () => cancelAnimationFrame(frame);
  }, [target, isNavigatorReady, fontsReady, done]);

  return { showSplash: !done, fontsReady };
}

/**
 * Устройство не влезло в лимит — показать выбор.
 *
 * <h2>Почему это здесь, а не на экране входа</h2>
 * В лимит можно упереться двумя путями: сразу после входа и на запуске
 * приложения, которое вошло раньше. Второй путь экран входа не видит
 * вовсе — человек туда просто не заходит.
 *
 * <h2>⚠️ Почему `replace`, а не `push`</h2>
 * Возвращаться кнопкой «назад» некуда: приложением пользоваться ещё
 * нельзя, и экран под ним показывал бы контент, к которому нет доступа.
 *
 * @param splashVisible пока splash сверху, переход не виден и только
 *                      сбил бы стартовый маршрут
 */
function useDeviceGuard(splashVisible: boolean): void {
  const status = useDeviceStore((s) => s.status);
  const navigationState = useRootNavigationState();
  const isNavigatorReady = Boolean(navigationState?.key);
  const pathname = usePathname();

  useEffect(() => {
    if (status !== 'limit' || splashVisible || !isNavigatorReady) return;

    /**
     * ⚠️ Уже на месте — второй переход не нужен.
     *
     * Со входа сюда приводит сам экран входа: он дожидается регистрации
     * устройства и уходит на `/devices` сам (`features/auth/store`).
     * Без этой проверки следом шёл бы ВТОРОЙ `router.replace` на тот же
     * адрес — а два перехода по корневому стеку в один кадр и давали
     * чёрный экран в собранной APK.
     *
     * За этим сторожем остаётся его настоящая работа: запуск
     * приложения, которое вошло раньше. Там экрана входа нет и увести
     * человека больше некому.
     */
    if (pathname === '/devices') return;

    router.replace('/devices');
  }, [status, splashVisible, isNavigatorReady, pathname]);
}
