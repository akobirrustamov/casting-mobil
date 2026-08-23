import { useCallback, useEffect, useState } from 'react';

/**
 * Yuklash / xato / ma'lumot holatlarini bitta joyda boshqaradi.
 *
 * Server-state kutubxonasi (TanStack Query) hozircha loyihada yo'q, shuning
 * uchun minimal hook. Kutubxona qo'shilsa - shu hook o'rniga o'tadi va
 * sahifalar o'zgarmaydi.
 */
export function useApi(fetcher, deps = []) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    fetcher()
      .then((res) => {
        if (!cancelled) setData(res);
      })
      .catch((err) => {
        if (!cancelled) setError(err);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, reloadKey]);

  return { data, error, loading, reload };
}
