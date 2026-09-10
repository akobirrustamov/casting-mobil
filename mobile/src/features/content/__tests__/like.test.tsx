/**
 * «Нравится» — откуда берётся число до ответа сети.
 *
 * ⚠️ Жалоба заказчика (10.09.2026): «лайки приходят слишком медленно».
 * Число с карточки ряда — последний, но мгновенный источник: оно уже в
 * кэше главной. Ответы страницы свежее и должны его перебивать.
 */
let mockCard: { likeCount: number | null } | undefined;

jest.mock('@/features/home/api', () => ({ useContentCard: () => mockCard }));
jest.mock('@/features/auth/store', () => ({
  useAuthStore: (select: (s: { token: string | null }) => unknown) =>
    select({ token: 't' }),
}));
jest.mock('@/features/watch/api', () => ({ setLike: jest.fn() }));
jest.mock('expo-router', () => ({ router: { push: jest.fn() } }));

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, create } from 'react-test-renderer';

import { useContentLike } from '../like';

let result: ReturnType<typeof useContentLike>;
function Probe(props: { detail?: { likeCount: number | null; liked: boolean } }) {
  result = useContentLike(42, props.detail as never, undefined);
  return null;
}

function render(detail?: { likeCount: number | null; liked: boolean }) {
  act(() => {
    create(
      <QueryClientProvider client={new QueryClient()}>
        <Probe detail={detail} />
      </QueryClientProvider>
    );
  });
}

it('до ответа страницы — число с карточки, сразу', () => {
  mockCard = { likeCount: 12 };
  render();

  expect(result.likes).toBe(12);
});

it('ответ страницы свежее карточки и перебивает её', () => {
  mockCard = { likeCount: 12 };
  render({ likeCount: 15, liked: true });

  expect(result.likes).toBe(15);
  expect(result.liked).toBe(true);
});

it('нет ни карточки, ни ответа — null (плитка покажет 0)', () => {
  mockCard = undefined;
  render();

  expect(result.likes).toBeNull();
});
