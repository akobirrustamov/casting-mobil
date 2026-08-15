import { getItem, removeItem, setItem } from '@/lib/storage';

/**
 * Признак «презентацию уже показывали».
 *
 * По макету онбординг — часть первого входа, поэтому со второго запуска
 * его быть не должно.
 */
const KEY = 'uzcasting.onboarding_seen';

export async function isOnboardingSeen(): Promise<boolean> {
  return (await getItem(KEY)) === '1';
}

export async function markOnboardingSeen(): Promise<void> {
  await setItem(KEY, '1');
}

/** Для отладки: сбросить, чтобы снова увидеть первый вход. */
export async function resetOnboarding(): Promise<void> {
  await removeItem(KEY);
}
