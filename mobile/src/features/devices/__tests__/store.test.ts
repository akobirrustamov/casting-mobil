/**
 * Регистрация устройства и предел в два устройства.
 *
 * <h2>⚠️ Почему это нужно тестами</h2>
 * Тут ошибаются в сторону, которую невозможно заметить на своём телефоне:
 * своё устройство всегда первое и в лимит не упирается. Сломанная ветка
 * «мест нет» проявится у ЧУЖОГО человека — он просто не сможет войти и
 * не поймёт почему.
 *
 * Три места, где легко ошибиться и дорого ошибиться:
 * отличить лимит от обрыва сети, не запереть человека при сбое сети,
 * не оставить статус от предыдущего аккаунта.
 */

jest.mock('@/lib/api', () => ({
  setDeviceId: jest.fn(),
  api: {},
}));

jest.mock('../installationId', () => ({
  installationId: jest.fn(async () => 'qurilma-1'),
  deviceLabel: jest.fn(() => 'iPhone'),
  devicePlatform: jest.fn(() => 'ios'),
}));

jest.mock('../api', () => ({
  registerDevice: jest.fn(),
  DeviceLimitError: class DeviceLimitError extends Error {
    constructor(message?: string) {
      super(message ?? 'limit');
      this.name = 'DeviceLimitError';
    }
  },
}));

import { setDeviceId } from '@/lib/api';

import { DeviceLimitError, registerDevice } from '../api';
import { useDeviceStore } from '../store';

const mockRegister = registerDevice as jest.MockedFunction<typeof registerDevice>;
const mockSetDeviceId = setDeviceId as jest.MockedFunction<typeof setDeviceId>;

const OK = { limit: 2, devices: [] };

beforeEach(() => {
  jest.clearAllMocks();
  useDeviceStore.getState().reset();
});

describe('Ключ устройства', () => {
  /**
   * ⚠️ Заголовок должен стоять ДО входа: бэкенд записывает устройство в
   * тот refresh-токен, который выдаёт на входе. Опоздав, мы получили бы
   * ничей токен — и «выйти с этого устройства» его бы не закрыло.
   */
  it('уходит в HTTP-клиент', async () => {
    await useDeviceStore.getState().prime();

    expect(mockSetDeviceId).toHaveBeenCalledWith('qurilma-1');
  });
});

describe('Регистрация', () => {
  it('успех переводит в registered', async () => {
    mockRegister.mockResolvedValue(OK);

    await expect(useDeviceStore.getState().ensureRegistered()).resolves.toBe('registered');
    expect(useDeviceStore.getState().status).toBe('registered');
  });

  it('лимит переводит в limit и сохраняет сообщение сервера', async () => {
    mockRegister.mockRejectedValue(new DeviceLimitError('2 tadan ortiq mumkin emas'));

    await expect(useDeviceStore.getState().ensureRegistered()).resolves.toBe('limit');

    const state = useDeviceStore.getState();
    expect(state.status).toBe('limit');
    // Сообщение приходит с сервера, потому что в нём названо число
    // устройств, а оно меняется в админке.
    expect(state.limitMessage).toBe('2 tadan ortiq mumkin emas');
  });

  /**
   * ⚠️ Обрыв сети НЕ должен запирать человека. Настоящий предел всё
   * равно применит бэкенд при следующем обновлении токена — а показать
   * «у вас слишком много устройств» тому, у кого просто пропал интернет,
   * значит соврать и отобрать доступ ни за что.
   */
  it('сетевая ошибка не выглядит как лимит', async () => {
    mockRegister.mockRejectedValue(new Error('Network Error'));

    await expect(useDeviceStore.getState().ensureRegistered()).resolves.toBe('error');
    expect(useDeviceStore.getState().status).toBe('error');
  });

  /**
   * Запуск приложения и восстановление сессии могут дёрнуть регистрацию
   * почти одновременно. Второй запрос не нужен: он занял бы место в
   * очереди и мог прийти с уже устаревшим ответом.
   */
  it('параллельный вызов не шлёт второй запрос', async () => {
    let release: (value: typeof OK) => void = () => {};
    mockRegister.mockReturnValue(new Promise((resolve) => {
      release = resolve;
    }));

    const first = useDeviceStore.getState().ensureRegistered();
    const second = useDeviceStore.getState().ensureRegistered();

    await expect(second).resolves.toBe('checking');
    release(OK);
    await first;

    expect(mockRegister).toHaveBeenCalledTimes(1);
  });
});

describe('Выход из аккаунта', () => {
  /**
   * ⚠️ Иначе следующий вошедший на этом телефоне увидел бы чужой
   * «лимит устройств» — экран выбора устройства вместо приложения.
   */
  it('сбрасывает статус', async () => {
    mockRegister.mockRejectedValue(new DeviceLimitError());
    await useDeviceStore.getState().ensureRegistered();
    expect(useDeviceStore.getState().status).toBe('limit');

    useDeviceStore.getState().reset();

    expect(useDeviceStore.getState().status).toBe('idle');
    expect(useDeviceStore.getState().limitMessage).toBeNull();
  });
});
