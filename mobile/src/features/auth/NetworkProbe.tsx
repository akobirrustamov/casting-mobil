import { useEffect, useState } from 'react';
import { Text } from 'react-native';

import { BASE_URL } from '@/lib/api';

/**
 * Проверка связи ИЗ САМОГО ПРИЛОЖЕНИЯ.
 *
 * <h2>Зачем</h2>
 * 03.09.2026 вход упёрся в `ERR_NETWORK`, и снаружи всё выглядело исправным:
 * DNS отдаёт нужный IP, сертификат валиден до корня, TLS 1.2 работает,
 * манифест разрешает интернет, обновления приложение качает. То есть с моей
 * машины сервер доступен, а с телефона — нет, и почему, по экрану не видно.
 *
 * Дальше можно было только гадать, поэтому спрашиваем у телефона напрямую:
 * два запроса, наш адрес и посторонний.
 *
 *   `api 200 · net 204`   связь есть, дело в конкретном запросе
 *   `api ERR · net 204`   интернет есть, до НАШЕГО сервера не доходит
 *   `api ERR · net ERR`   у телефона нет сети вообще
 *
 * Второй адрес — `generate_204` у Google: он отвечает пустым `204` и его же
 * использует сам Android для проверки соединения.
 *
 * ⚠️ Временно, на время разбора входа. Убрать вместе с `BuildMarker`.
 */
export function NetworkProbe() {
  const [line, setLine] = useState('...');

  useEffect(() => {
    let alive = true;

    const probe = async (url: string) => {
      try {
        const r = await fetch(url, { method: 'GET' });
        return String(r.status);
      } catch (e) {
        // Текст сетевой ошибки в RN короткий и по делу
        // («Network request failed», «SSL handshake aborted…»).
        return (e instanceof Error ? e.message : 'ERR').slice(0, 60);
      }
    };

    (async () => {
      const api = await probe(`${BASE_URL}/api/v1/app/home`);
      const net = await probe('https://www.google.com/generate_204');
      if (alive) setLine(`api ${api} · net ${net}`);
    })();

    return () => {
      alive = false;
    };
  }, []);

  return (
    <Text className="text-center text-caption text-text-muted opacity-60">{line}</Text>
  );
}
