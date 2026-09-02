import { useQuery } from '@tanstack/react-query';
import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { PosterCard } from '@/components/ui/PosterCard';
import { Rail } from '@/components/ui/Rail';
import { CARD_RATIO, useRailCardWidth } from '@/features/content/railLayout';
import { useFeedLanguage } from '@/features/home/api';
import { DEFAULT_LANGUAGE, type Language } from '@/i18n';
import { mediaUrl } from '@/lib/api';

import { useViewerKey } from './api';
import { fetchContinueWatching, type ContinueItem } from './progressApi';

const LOCALE_PARAM: Record<Language, 'UZ' | 'RU' | 'EN'> = {
  uz: 'UZ',
  ru: 'RU',
  en: 'EN',
};

/**
 * Ряд «Продолжить просмотр» на главной.
 *
 * <h2>⚠️ Ряда просто НЕТ, когда продолжать нечего</h2>
 * Не пустая полоса и не заглушка «здесь пока ничего»: у нового
 * человека этот ряд занимал бы место в самом верху главной и
 * сообщал бы ровно ноль. Пустое состояние здесь — отсутствие блока.
 *
 * <h2>⚠️ Только для вошедших</h2>
 * Позиция принадлежит человеку, а не устройству. Гостю сервер вернёт
 * отказ, и запрос не отправляется вовсе — иначе на каждой главной
 * висел бы заведомо неудачный вызов.
 *
 * <h2>Куда ведёт нажатие</h2>
 * На экран контента, как и остальные карточки главной. Прыгать сразу
 * в плеер заманчиво, но право на просмотр к этому моменту могло
 * измениться (подписка кончилась, серию сняли с публикации) — решает
 * это `/watch`, а спрашивает его экран контента.
 *
 * Сама позиция при этом не теряется: плеер возьмёт её из
 * `useWatchProgress`, когда откроется.
 */
export function ContinueRail() {
  const { t } = useTranslation();
  const language = useFeedLanguage();
  const viewer = useViewerKey();
  const cardWidth = useRailCardWidth();

  const query = useQuery({
    queryKey: ['watch-progress', 'continue', language, viewer],
    queryFn: () => fetchContinueWatching(LOCALE_PARAM[language ?? DEFAULT_LANGUAGE]),

    // ⚠️ Гостю не запрашиваем: позиция привязана к человеку, ответ
    // был бы отказом на каждой загрузке главной.
    enabled: viewer !== 'guest',

    // Человек мог посмотреть что-то и вернуться на главную. Показать
    // при этом вчерашнюю позицию — заметная неправда: ряд именно про
    // «где я остановился».
    staleTime: 0,

    // ⚠️ Молча: ряд необязательный. Ошибка сети не должна оставлять
    // на главной красный блок вместо контента — остальные ряды
    // работают.
    retry: 1,
  });

  const items = query.data ?? [];

  // Нет ответа, ошибка или продолжать нечего — блока нет совсем.
  if (items.length === 0) {
    return null;
  }

  return (
    <Rail title={t('home.continueWatching')} icon="play-circle-outline">
      {items.map((item) => (
        <ContinueCard key={cardKey(item)} item={item} width={cardWidth} />
      ))}
    </Rail>
  );
}

/**
 * ⚠️ Ключ из ТИПА и номера, а не из одного номера.
 *
 * У серии и у контента идентификаторы нумеруются независимо, и
 * `EPISODE 7` вполне может соседствовать с `CONTENT 7`. По одному
 * номеру React считал бы их одной карточкой и показал бы только одну.
 */
function cardKey(item: ContinueItem): string {
  return `${item.progress.type}-${item.progress.targetId}`;
}

function ContinueCard({ item, width }: { item: ContinueItem; width: number }) {
  const { t } = useTranslation();
  const { content, progress, episodeNumber } = item;

  return (
    <PosterCard
      width={width}
      ratio={CARD_RATIO}
      title={content.title ?? ''}
      // ⚠️ Вместо описания — номер серии: в этом ряду важно не «про что
      // это», а «на чём я остановился». Описание человек уже читал,
      // когда открывал контент в первый раз.
      subtitle={
        episodeNumber !== null
          ? t('content.part', { number: episodeNumber })
          : (content.shortDescription ?? undefined)
      }
      meta={content.genre ?? undefined}
      imageUrl={mediaUrl(content.posterMediaId)}
      progressPercent={progress.percent}
      onPress={() => router.push(`/content/${content.id}`)}
    />
  );
}
