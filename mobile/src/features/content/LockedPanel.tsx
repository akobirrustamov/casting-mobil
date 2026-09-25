import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Text, View } from 'react-native';

import { pushOnce } from '@/lib/navigation';
import { Button } from '@/components/ui/Button';
import type { RequiredAction, WatchInfo } from '@/features/watch/types';
import { formatSum } from '@/lib/money';
import { colors } from '@/theme/tokens';
import { usePaymentsVisible } from '@/features/config/api';

/**
 * Почему закрыто и что с этим делать.
 *
 * ⚠️ Правило доступа считает ТОЛЬКО сервер (`AccessService`, ТЗ §37).
 * Экран ничего не досчитывает: складывай он подписку с покупкой сам,
 * правило жило бы в двух местах и разъехалось бы при первом изменении
 * тарифов.
 *
 * Кнопка рисуется только там, где ей есть куда вести. Вход есть, экрана
 * оплаты пока нет — он ждёт решения по оплате через сторы. Поэтому там,
 * где нужна оплата, показывается НАСТОЯЩАЯ цена сервера, а на нажатие
 * кнопка честно отвечает, что оплата ещё не подключена.
 */

/** Что делать — по требуемому действию. */
const LOCKED_BODY: Record<string, string> = {
  SIGN_IN: 'content.needSignIn',
  BUY_EPISODE: 'content.needPurchase',
  BUY_PREMIERE: 'content.needPurchase',
  SUBSCRIBE: 'content.needSubscription',
  BUY_OR_SUBSCRIBE: 'content.needPurchaseOrSubscription',
};

/**
 * Отказы, в которых делать нечего: `requiredAction` там `NONE`, и без этой
 * таблицы человек увидел бы общее «закрыто» вместо настоящей причины.
 */
const LOCKED_REASON: Record<string, string> = {
  NOT_PUBLISHED: 'content.notPublished',
  USER_BLOCKED: 'content.userBlocked',
};

export function LockedPanel({ info }: { info: WatchInfo }) {
  const { t } = useTranslation();
  const action: RequiredAction = info.requiredAction;

  const [paymentNote, setPaymentNote] = useState(false);
  // Платежи выключены в админке — ни цены, ни «купить/подписаться»,
  // только «закрыто».
  const paymentsVisible = usePaymentsVisible();

  const needsSignIn = action === 'SIGN_IN';
  const needsPurchase =
    paymentsVisible &&
    (action === 'BUY_EPISODE' || action === 'BUY_PREMIERE' || action === 'BUY_OR_SUBSCRIBE');
  const needsSubscription =
    paymentsVisible && (action === 'SUBSCRIBE' || action === 'BUY_OR_SUBSCRIBE');

  const showActionBody = paymentsVisible || needsSignIn;
  const bodyKey =
    (showActionBody ? LOCKED_BODY[action] : undefined) ??
    LOCKED_REASON[info.reason] ??
    'states.lockedBody';
  const price = info.episodePrice ?? info.premierePrice;

  return (
    <View className="gap-3 rounded-card bg-surface p-4">
      <View className="flex-row items-center gap-2">
        <Ionicons name="lock-closed" size={16} color={colors.gold} />
        <Text className="text-h2 text-text">{t('states.lockedTitle')}</Text>
      </View>

      <Text className="text-body text-text-muted">{t(bodyKey)}</Text>

      {needsSignIn ? (
        <Button onPress={() => pushOnce('/(auth)/sign-in')}>{t('profile.signIn')}</Button>
      ) : null}

      {needsPurchase ? (
        // Ромб и цена — форма кнопки покупки с макета заказчика.
        // Слово «купить» на ней лишнее: цена и знак говорят то же самое.
        <Button
          variant="purchase"
          onPress={() => setPaymentNote(true)}
          leading={<Ionicons name="diamond" size={16} color={colors.white} />}
        >
          {price === null ? t('common.buy') : t('common.price', { amount: formatSum(price) })}
        </Button>
      ) : null}

      {needsSubscription ? (
        <Button variant="gold" onPress={() => setPaymentNote(true)}>
          {t('content.subscribe')}
        </Button>
      ) : null}

      {paymentNote ? (
        <Text className="text-micro text-text-muted">{t('content.paymentSoon')}</Text>
      ) : null}
    </View>
  );
}
