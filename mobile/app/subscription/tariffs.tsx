import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Text, View } from 'react-native';

import { ScreenState } from '@/components/states/ScreenState';
import { Button } from '@/components/ui/Button';
import { Screen } from '@/components/ui/Screen';
import { useTariffs, type Tariff } from '@/features/subscription/api';
import { formatSum } from '@/lib/money';
import { colors, gradients, radius } from '@/theme/tokens';

/**
 * Тарифы Premium — карточки с ценами.
 *
 * <h2>Кто видит</h2>
 * Все, включая гостя: цена — то, что смотрят ДО входа. Бэкенд отдаёт
 * `/app/tariffs` без токена по той же причине.
 *
 * <h2>⚠️ Кнопка «Tanlash» пока не покупает</h2>
 * Платёжный провайдер не подключён (Payme / Click / Uzum — реквизитов
 * нет, см. план, шаг 24). Кнопка стоит выключенной с честной подписью,
 * а не ведёт на экран, который «почти работает»: живая кнопка, за
 * которой ничего нет, читается как поломка и подрывает доверие к ценам
 * рядом с ней.
 *
 * <h2>Числа — с сервера</h2>
 * Цена, «в месяц» и «самый выгодный» приходят из админки. Здесь ничего
 * не считается и не захардкожено: заказчик меняет цены без релиза.
 */
export default function TariffsScreen() {
  const { t } = useTranslation();
  const tariffs = useTariffs();

  return (
    <Screen
      title={t('subscription.tariffsTitle')}
      subtitle={t('subscription.tariffsSubtitle')}
      onBack={() => router.back()}
      onRefresh={() => void tariffs.refetch()}
      refreshing={tariffs.isRefetching}
    >
      {tariffs.isLoading ? <ScreenState kind="loading" /> : null}

      {tariffs.isError ? (
        <ScreenState kind="error" onRetry={() => void tariffs.refetch()} />
      ) : null}

      {tariffs.data?.length === 0 ? (
        <ScreenState kind="empty" body={t('subscription.tariffsEmpty')} />
      ) : null}

      <View className="gap-4">
        {tariffs.data?.map((tariff) => <TariffCard key={tariff.id} tariff={tariff} />)}
      </View>

      {tariffs.data && tariffs.data.length > 0 ? (
        <Text className="text-center text-caption text-text-muted">
          {t('subscription.paymentSoon')}
        </Text>
      ) : null}
    </Screen>
  );
}

/**
 * Карточка тарифа.
 *
 * Выделенный тариф («ENG FOYDALI») стоит на premium-градиенте — том же,
 * что у баннера в профиле. Остальные — на обычной поверхности: на экране
 * должно быть видно, КАКОЙ из них заказчик хочет продать.
 */
function TariffCard({ tariff }: { tariff: Tariff }) {
  const { t } = useTranslation();

  const price = t('common.price', { amount: formatSum(tariff.price) });

  // «В месяц» имеет смысл только для многомесячных: у месячного тарифа
  // эта строка повторяла бы цену.
  const monthly =
    tariff.durationMonths > 1 && tariff.monthlyPrice != null
      ? t('subscription.perMonth', {
          amount: t('common.price', { amount: formatSum(tariff.monthlyPrice) }),
        })
      : null;

  const body = (
    <View className="gap-4 p-4">
      <View className="flex-row items-start justify-between gap-3">
        <View className="flex-1 gap-1">
          {tariff.badge ? (
            <View className="flex-row items-center gap-1 self-start rounded-pill bg-gold px-2.5 py-1">
              <Ionicons name="star" size={11} color={colors.ink} />
              <Text className="text-micro font-semibold text-ink">{tariff.badge}</Text>
            </View>
          ) : null}
          <Text className="text-h2 text-text">{tariff.name}</Text>
          <Text className="text-caption text-text-muted">
            {t('subscription.months', { count: tariff.durationMonths })}
          </Text>
        </View>

        <View className="items-end">
          <Text className="text-h2 text-text">{price}</Text>
          {monthly ? <Text className="text-micro text-text-muted">{monthly}</Text> : null}
        </View>
      </View>

      {tariff.description ? (
        <Text className="text-caption text-text-muted">{tariff.description}</Text>
      ) : null}

      {tariff.features.length > 0 ? (
        <View className="gap-2">
          {tariff.features.map((feature) => (
            <View key={feature} className="flex-row items-start gap-2">
              <Ionicons
                name="checkmark-circle"
                size={16}
                color={tariff.highlighted ? colors.gold : colors.success}
                style={{ marginTop: 2 }}
              />
              <Text className="flex-1 text-body text-text">{feature}</Text>
            </View>
          ))}
        </View>
      ) : null}

      {/* ⚠️ Выключена намеренно — см. шапку файла. */}
      <Button variant={tariff.highlighted ? 'gold' : 'secondary'} shape="card" disabled>
        {t('subscription.select')}
      </Button>
    </View>
  );

  if (!tariff.highlighted) {
    return <View className="rounded-card-lg bg-surface">{body}</View>;
  }

  // Градиент — только кромка: сплошная заливка делала бы цену и список
  // нечитаемыми на ярком фоне.
  return (
    <LinearGradient
      colors={gradients.premium}
      start={{ x: 0, y: 0 }}
      end={{ x: 1, y: 1 }}
      style={{ borderRadius: radius.cardLg, padding: 2 }}
    >
      <View style={{ borderRadius: radius.cardLg - 2, backgroundColor: colors.surface }}>
        {body}
      </View>
    </LinearGradient>
  );
}
