/**
 * Обходной путь для счётчиков сериала.
 *
 * ⚠️ Главное — чтобы на новом сервере он НЕ делал запросов: два лишних
 * похода в сеть на каждом открытии сериала ради данных, которые уже
 * пришли в карточке.
 */
const mockUseEpisodes = jest.fn();
const mockUseWatchEpisode = jest.fn();

jest.mock('@/features/watch/episodes', () => ({
  useEpisodes: (id: number | null) => mockUseEpisodes(id),
}));
jest.mock('@/features/watch/api', () => ({
  useWatchEpisode: (id: number | null) => mockUseWatchEpisode(id),
}));

import { act, create } from 'react-test-renderer';

import { useSerialCountersFallback } from '../serialCounters';

let result: unknown;
function Probe({ enabled }: { enabled: boolean }) {
  result = useSerialCountersFallback(42, enabled);
  return null;
}

beforeEach(() => {
  mockUseEpisodes.mockReset();
  mockUseWatchEpisode.mockReset();
  mockUseEpisodes.mockReturnValue({ data: { episodes: [{ id: 11 }, { id: 12 }] } });
  mockUseWatchEpisode.mockReturnValue({ data: { contentId: 42, likeCount: 5 } });
});

it('выключен — оба запроса отключены (null), результата нет', () => {
  act(() => {
    create(<Probe enabled={false} />);
  });

  expect(mockUseEpisodes).toHaveBeenLastCalledWith(null);
  expect(mockUseWatchEpisode).toHaveBeenLastCalledWith(null);
  expect(result).toBeUndefined();
});

it('включён — спрашивает ПЕРВУЮ серию и отдаёт её счётчики', () => {
  act(() => {
    create(<Probe enabled />);
  });

  expect(mockUseEpisodes).toHaveBeenLastCalledWith(42);
  expect(mockUseWatchEpisode).toHaveBeenLastCalledWith(11);
  expect(result).toEqual({ contentId: 42, likeCount: 5 });
});
