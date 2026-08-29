/**
 * Длительность для бейджа на постере — `45:12`, `01:12:45`.
 *
 * <h2>Почему не «90 мин»</h2>
 * На макете заказчика в углу обложки стоит именно таймкод. Минуты словом
 * читаются как справка, таймкод — как длительность ролика, и это то, что
 * человек ожидает увидеть на карточке видео.
 *
 * Часы появляются только когда они есть: у трёхминутного клипа `00:03:45`
 * выглядело бы как ошибка вёрстки.
 */
export function formatDuration(seconds: number | null | undefined): string | null {
  if (typeof seconds !== 'number' || !Number.isFinite(seconds) || seconds <= 0) {
    return null;
  }

  const total = Math.round(seconds);
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;

  const pad = (n: number) => String(n).padStart(2, '0');

  return h > 0 ? `${pad(h)}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`;
}
