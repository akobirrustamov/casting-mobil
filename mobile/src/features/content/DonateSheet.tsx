import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal, Pressable, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/Button';
import { useAuthStore } from '@/features/auth/store';
import { useBalance } from '@/features/profile/api';
import { groupDigits } from '@/lib/money';
import { TOUCH_TARGET, colors } from '@/theme/tokens';

/**
 * Окно доната — открывается кнопкой «Donat qilish» на странице контента.
 *
 * <h2>⚠️ Отправка пока НЕ подключена — и это видно на самом окне</h2>
 * Эндпоинт `POST /api/v1/app/donations` на бэкенде есть и работает, но
 * списывает он с ВНУТРЕННЕГО баланса, а пополнить его в приложении нечем:
 * покупка пакетов идёт через платёжного провайдера, которого пока нет
 * (`PackagePurchaseService` отвечает 503 на `PAYMENT_SYSTEM`).
 *
 * То есть рабочая кнопка «отправить» у человека с нулём звёзд просто
 * вернула бы отказ «недостаточно средств» — и он решил бы, что сломалось
 * приложение, а не что оплата ещё не открыта. Поэтому окно честно говорит,
 * когда это заработает.
 *
 * ⚠️ Само окно при этом настоящее: выбор суммы и баланс — то, что
 * останется, когда оплату подключат. Меняться будет только обработчик
 * кнопки.
 */

/**
 * Сколько звёзд предлагаем.
 *
 * Ряд, а не поле ввода: набирать число на клавиатуре ради поддержки —
 * лишнее движение, а на макете стоят готовые суммы.
 */
const PRESETS = [10, 50, 100, 500] as const;

export function DonateSheet({
  open,
  contentId,
  title,
  onClose,
}: {
  open: boolean;
  contentId: number | null;
  title: string | null;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const insets = useSafeAreaInsets();
  const signedIn = useAuthStore((s) => s.token !== null);

  const [amount, setAmount] = useState<number>(PRESETS[1]);
  const [note, setNote] = useState(false);

  // Следующее открытие начинается с обычной подписи, а не с чужого
  // результата прошлого нажатия.
  useEffect(() => {
    if (!open) setNote(false);
  }, [open]);

  // Баланс спрашиваем только у вошедшего: гостю этот адрес ответит 401,
  // и в списке запросов остался бы вечный красный.
  const balance = useBalance();

  return (
    <Modal
      visible={open}
      transparent
      animationType="slide"
      onRequestClose={onClose}
      statusBarTranslucent
    >
      <Pressable className="flex-1 justify-end bg-black/60" onPress={onClose}>
        {/* Нажатие внутри окна не должно его закрывать. */}
        <Pressable
          onPress={(e) => e.stopPropagation()}
          style={{ paddingBottom: insets.bottom + 16 }}
          className="gap-4 rounded-t-card-lg bg-surface px-4 pt-3"
        >
          <View className="h-1 w-10 self-center rounded-pill bg-border" />

          <View className="flex-row items-start gap-2">
            <View className="flex-1">
              <Text className="text-h2 text-text">{t('content.donate')}</Text>
              {title ? (
                <Text numberOfLines={1} className="mt-1 text-caption text-text-muted">
                  {title}
                </Text>
              ) : null}
            </View>

            <Pressable
              onPress={onClose}
              accessibilityRole="button"
              hitSlop={12}
              style={{ width: TOUCH_TARGET - 12, height: TOUCH_TARGET - 12 }}
              className="items-center justify-center active:opacity-60"
            >
              <Ionicons name="close" size={22} color={colors.textMuted} />
            </Pressable>
          </View>

          {signedIn && balance.data ? (
            <View className="flex-row items-center gap-2 rounded-card bg-surface-2 px-3 py-2">
              <Ionicons name="star" size={14} color={colors.gold} />
              <Text className="text-caption text-text-muted">
                {t('content.yourStars', { amount: groupDigits(balance.data.stars) })}
              </Text>
            </View>
          ) : null}

          <View className="flex-row flex-wrap gap-2">
            {PRESETS.map((value) => (
              <Pressable
                key={value}
                onPress={() => setAmount(value)}
                accessibilityRole="button"
                accessibilityState={{ selected: value === amount }}
                className={`flex-row items-center gap-1.5 rounded-pill px-4 py-2.5 ${
                  value === amount ? 'bg-purple' : 'bg-surface-2'
                }`}
              >
                <Ionicons
                  name="star"
                  size={13}
                  color={value === amount ? colors.white : colors.gold}
                />
                <Text
                  className={`text-caption ${
                    value === amount ? 'font-semibold text-white' : 'text-text-muted'
                  }`}
                >
                  {groupDigits(value)}
                </Text>
              </Pressable>
            ))}
          </View>

          {signedIn ? (
            <Button
              variant="purchase"
              onPress={() => setNote(true)}
              leading={<Ionicons name="heart" size={16} color={colors.white} />}
              disabled={contentId === null}
            >
              {t('content.donateAmount', { amount: groupDigits(amount) })}
            </Button>
          ) : (
            <Button
              onPress={() => {
                onClose();
                router.push('/(auth)/sign-in');
              }}
            >
              {t('profile.signIn')}
            </Button>
          )}

          {note ? (
            <Text className="text-micro text-text-muted">{t('content.donateSoon')}</Text>
          ) : (
            <Text className="text-micro text-text-disabled">{t('content.donateHint')}</Text>
          )}
        </Pressable>
      </Pressable>
    </Modal>
  );
}
