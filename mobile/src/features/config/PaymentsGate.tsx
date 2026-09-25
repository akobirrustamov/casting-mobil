import { Redirect } from 'expo-router';
import type { ComponentType } from 'react';

import { useAppConfig } from './api';

/**
 * Платёжный экран открывается только когда платежи включены в админке.
 *
 * Входы в эти экраны и так скрыты; обёртка закрывает прямые ссылки
 * (push-уведомление, deep link). Пока ответа нет — пусто, а не редирект:
 * иначе при включённых платежах экран закрывался бы, не успев открыться.
 */
export function withPaymentsGate<P extends object>(Screen: ComponentType<P>) {
  function Gated(props: P) {
    const config = useAppConfig();
    if (config.data?.paymentsVisible === true) return <Screen {...props} />;
    if (config.isPending) return null;
    return <Redirect href="/profile" />;
  }
  Gated.displayName = `withPaymentsGate(${Screen.displayName ?? Screen.name ?? 'Screen'})`;
  return Gated;
}
