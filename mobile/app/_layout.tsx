import '@/i18n';
import '../global.css';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
// SDK 56+: react-navigation импортируется только через expo-router.
// Прямой @react-navigation/native ломает бандл.
import { Stack, router, useRootNavigationState } from 'expo-router';
import { ThemeProvider } from 'expo-router/react-navigation';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { SplashOverlay } from '@/components/SplashOverlay';
import { useAuthStore } from '@/features/auth/store';
import { isOnboardingSeen } from '@/features/onboarding/store';
import i18nInstance from '@/i18n';
import { loadLanguage } from '@/i18n/storage';
import { colors, navigationTheme } from '@/theme/tokens';

/** По подписи к макету splash висит 1–2 секунды. */
const SPLASH_MIN_MS = 1300;

export default function RootLayout() {
  const { t } = useTranslation();
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

  const showSplash = useBootstrap();

  return (
    <GestureHandlerRootView style={{ flex: 1, backgroundColor: colors.ink }}>
      <SafeAreaProvider>
        <QueryClientProvider client={queryClient}>
          <ThemeProvider value={navigationTheme}>
            {/* ТЗ: dark mode первичен, светлой темы нет */}
            <StatusBar style="light" />
            <Stack
              screenOptions={{
                headerShown: false,
                contentStyle: { backgroundColor: colors.ink },
              }}
            >
              <Stack.Screen name="(tabs)" />
              <Stack.Screen name="onboarding" />
              <Stack.Screen name="(auth)" />
            </Stack>

            {showSplash ? <SplashOverlay subtitle={t('splash.subtitle')} /> : null}
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
 * @returns показывать ли splash
 */
function useBootstrap(): boolean {
  const restore = useAuthStore((s) => s.restore);
  const navigationState = useRootNavigationState();
  const isNavigatorReady = Boolean(navigationState?.key);

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
  }, [restore]);

  useEffect(() => {
    if (!target || !isNavigatorReady || done) return;

    // '/(tabs)' — стартовый маршрут, переходить никуда не нужно
    if (target !== '/(tabs)') {
      router.replace(target);
    }

    // Снимаем оверлей следующим кадром, когда новый экран уже отрисован,
    // иначе на мгновение мелькает то, что было под ним.
    const frame = requestAnimationFrame(() => setDone(true));
    return () => cancelAnimationFrame(frame);
  }, [target, isNavigatorReady, done]);

  return !done;
}
