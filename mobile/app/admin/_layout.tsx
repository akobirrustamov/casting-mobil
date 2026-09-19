import { Stack } from 'expo-router';
import { useEffect } from 'react';

import { ScreenState } from '@/components/states/ScreenState';
import { useAdminStore } from '@/features/castingAdmin/store';
import { colors } from '@/theme/tokens';

/**
 * Раздел админки кастинга.
 *
 * Сессию админа читаем здесь, при первом входе в раздел, а не на старте
 * приложения: подавляющее большинство запусков — это кандидаты и
 * зрители, и лишнее чтение SecureStore на splash им ни к чему.
 *
 * ⚠️ Навигатор монтируется только после чтения. Иначе экран списка
 * успел бы увидеть «токена нет», увёл бы на вход, и сразу за ним
 * восстановленная сессия увела бы обратно — два перехода подряд.
 *
 * Сторожа «нет сессии → на вход» стоят в самих экранах (`<Redirect>`),
 * а не здесь: экран входа живёт в этом же стеке, и сторож в раскладке
 * перенаправлял бы и его самого.
 */
export default function AdminLayout() {
  const isRestoring = useAdminStore((s) => s.isRestoring);
  const restore = useAdminStore((s) => s.restore);

  useEffect(() => {
    void restore();
  }, [restore]);

  if (isRestoring) return <ScreenState kind="loading" />;

  return (
    <Stack
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: colors.ink },
      }}
    />
  );
}
