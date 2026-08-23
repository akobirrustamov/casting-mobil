import * as Network from 'expo-network';

/**
 * Есть ли сеть.
 *
 * ТЗ требует состояние `offline` на каждом экране. Без него пропавший
 * интернет выглядит как «ошибка сервера» — человек жмёт «повторить»
 * и получает то же самое, не понимая причины.
 *
 * `isInternetReachable` важнее `isConnected`: к Wi-Fi можно быть подключённым,
 * а интернета не иметь — типичный кейс в кафе с капча-порталом. Пока значение
 * ещё не определено (undefined), считаем, что сеть есть: показывать «нет
 * интернета» на старте, до первой проверки, хуже, чем не показать вовсе.
 */
export function useIsOffline(): boolean {
  const state = Network.useNetworkState();

  if (state.isInternetReachable === false) return true;
  if (state.isConnected === false) return true;
  return false;
}
