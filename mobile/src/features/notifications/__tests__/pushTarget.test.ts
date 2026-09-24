/**
 * Нажатие на push ведёт туда же, куда карточка в «Xabarlar».
 *
 * Данные приходят от бэкенда (`NotificationPushService.payload`), и
 * числа там могут оказаться строками — FCM на Android превращает всё
 * в строки. Неразобранный id молча уводил бы в список вместо фильма.
 */
const mockPush = jest.fn();
const mockTrack = jest.fn();

jest.mock('expo-router', () => ({
  router: { push: (...a: unknown[]) => mockPush(...a) },
  useRootNavigationState: () => ({ key: 'k' }),
}));
jest.mock('@/features/analytics/api', () => ({ track: (...a: unknown[]) => mockTrack(...a) }));
jest.mock('@/features/auth/store', () => ({ useAuthStore: jest.fn() }));
jest.mock('@/features/devices/store', () => ({ useDeviceStore: jest.fn() }));
jest.mock('../pushToken', () => ({ registerPushToken: jest.fn() }));
jest.mock('expo-notifications', () => ({}));
jest.mock('@/lib/api', () => ({ api: {}, mediaUrl: () => undefined }));
jest.mock('@/features/home/api', () => ({ feedLocale: () => 'UZ' }));

import { Linking } from 'react-native';

import { openPushTarget } from '../push';

const mockOpenURL = jest.spyOn(Linking, 'openURL').mockResolvedValue(true);

beforeEach(() => {
  mockPush.mockClear();
  mockTrack.mockClear();
  mockOpenURL.mockClear();
});

test('ichki havola — kontentga', () => {
  openPushTarget({ notificationId: 7, linkType: 'INTERNAL', targetType: 'CONTENT', targetId: 42 });

  expect(mockPush).toHaveBeenCalledWith('/content/42');
  expect(mockTrack).toHaveBeenCalledWith({ type: 'NOTIFICATION_OPEN', targetId: 7 });
  expect(mockTrack).toHaveBeenCalledWith({ type: 'NOTIFICATION_CLICK', targetId: 7 });
});

test('satr ko‘rinishidagi id ham tushuniladi', () => {
  openPushTarget({ notificationId: '7', linkType: 'INTERNAL', targetType: 'EPISODE', targetId: '5' });

  expect(mockPush).toHaveBeenCalledWith('/episode/5');
});

test('tashqi havola — brauzerda', () => {
  openPushTarget({ notificationId: 1, linkType: 'EXTERNAL', linkUrl: 'https://uzcasting.com' });

  expect(mockOpenURL).toHaveBeenCalledWith('https://uzcasting.com');
  expect(mockPush).not.toHaveBeenCalled();
});

test('havolasiz — «Xabarlar» ro‘yxati', () => {
  openPushTarget({ notificationId: 3, linkType: 'NONE' });

  expect(mockPush).toHaveBeenCalledWith('/messages');
  expect(mockTrack).toHaveBeenCalledWith({ type: 'NOTIFICATION_OPEN', targetId: 3 });
  expect(mockTrack).not.toHaveBeenCalledWith({ type: 'NOTIFICATION_CLICK', targetId: 3 });
});
