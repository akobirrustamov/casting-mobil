import { create } from 'zustand';

import { setDeviceId } from '@/lib/api';

import { registerDevice, DeviceLimitError } from './api';
import { installationId } from './installationId';

/**
 * Состояние регистрации устройства.
 *
 * <h2>Почему это отдельный стор, а не поле в `auth/store`</h2>
 * Вход и лимит устройств — разные вещи, и смешивать их опасно: человек с
 * действующим токеном, упёршийся в лимит, НЕ разлогинен. Ему нужно открыть
 * список и освободить место — а для этого нужен рабочий токен.
 *
 * <h2>Как приложение относится к `limit`</h2>
 * Токен есть, но приложение не пускает дальше экрана устройств. Формально
 * человек авторизован; по смыслу — ещё нет.
 *
 * ⚠️ Настоящая защита не здесь. Клиент можно обойти, поэтому бэкенд
 * закрывает выгнанное устройство на обновлении токена
 * (`RefreshTokenService.ensureDeviceActive`): сессия живёт максимум до
 * конца текущего access-токена.
 */
export type DeviceStatus =
  /** Ещё не спрашивали. */
  | 'idle'
  /** Запрос в пути. */
  | 'checking'
  /** Устройство в списке — можно работать. */
  | 'registered'
  /** Мест нет: показать выбор устройства. */
  | 'limit'
  /** Сеть или старая сборка бэкенда — молча пропускаем. */
  | 'error';

type DeviceState = {
  status: DeviceStatus;
  /** Сообщение сервера про лимит — в нём названо число устройств. */
  limitMessage: string | null;

  /** Прочитать ключ установки и отдать его HTTP-клиенту. */
  prime: () => Promise<void>;
  /** Сообщить бэкенду об устройстве. */
  ensureRegistered: () => Promise<DeviceStatus>;
  /** После того как место освободили — попробовать снова. */
  reset: () => void;
};

export const useDeviceStore = create<DeviceState>((set, get) => ({
  status: 'idle',
  limitMessage: null,

  /**
   * ⚠️ Вызывается ДО входа.
   *
   * Бэкенд записывает устройство в тот refresh-токен, который выдаёт на
   * входе. Если заголовка в этот момент нет, токен остаётся ничей — и
   * «выйти с этого устройства» его уже не закроет.
   */
  prime: async () => {
    setDeviceId(await installationId());
  },

  ensureRegistered: async () => {
    if (get().status === 'checking') return 'checking';
    set({ status: 'checking' });

    try {
      await registerDevice();
      set({ status: 'registered', limitMessage: null });
      return 'registered';
    } catch (error) {
      if (error instanceof DeviceLimitError) {
        set({ status: 'limit', limitMessage: error.message });
        return 'limit';
      }
      // Сеть отвалилась или сервер старой сборки. Запирать человека
      // из-за этого нельзя: лимит всё равно применит бэкенд при
      // следующем обновлении токена.
      set({ status: 'error', limitMessage: null });
      return 'error';
    }
  },

  reset: () => set({ status: 'idle', limitMessage: null }),
}));
