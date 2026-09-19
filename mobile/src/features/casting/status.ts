import type { BadgeTone } from '@/components/ui/Badge';

/**
 * Статус заявки → что показать кандидату.
 *
 * Логика отдельно от экрана по той же причине, что и форма: перепутать
 * «одобрено без цены» с «одобрено с ценой» или показать «Katalogda
 * ko'rinmoqda» у отклонённой анкеты — ошибка, которую видно только на
 * конкретных данных. Тест проверяет все ветки.
 */
export type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

/** Одна заявка — `GET /api/v1/app/casting/applications/my`. */
export type MyApplication = {
  id: number;
  castingType: string;
  name: string;
  status: ApplicationStatus;
  /** Сумма в сумах; `null` — админ ещё не назначил. */
  price: number | null;
  paid: boolean;
  isWebShow: boolean;
  createdAt: string;
  /** UUID вложений — `fileUrl(id)`. */
  photos: string[];
};

export type ApplicationStatusView = {
  /** Ключ перевода подписи статуса. */
  labelKey: string;
  tone: BadgeTone;
  /** Цена показывается только у одобренной заявки. */
  price: number | null;
  /**
   * Ключ пояснения под статусом. Для одобренной — «To'lov — tez orada»:
   * оплаты в приложении пока нет, и честнее сказать «скоро», чем
   * нарисовать кнопку, которая никуда не ведёт.
   */
  noteKey: string | null;
  showInCatalog: boolean;
};

const STATUSES: ApplicationStatus[] = ['PENDING', 'APPROVED', 'REJECTED'];

/**
 * Незнакомый статус считаем «на рассмотрении».
 *
 * ⚠️ Не «отклонено» и не «одобрено»: если бэкенд добавит новое
 * состояние, кандидат не должен ни расстроиться зря, ни увидеть цену,
 * которой нет.
 */
export function normalizeStatus(raw: unknown): ApplicationStatus {
  const value = String(raw ?? '').toUpperCase();
  return (STATUSES as string[]).includes(value) ? (value as ApplicationStatus) : 'PENDING';
}

export function applicationStatusView(app: Pick<MyApplication, 'status' | 'price' | 'paid' | 'isWebShow'>): ApplicationStatusView {
  const status = normalizeStatus(app.status);

  if (status === 'REJECTED') {
    return {
      labelKey: 'casting.status.rejected',
      tone: 'locked',
      price: null,
      noteKey: 'casting.status.rejectedNote',
      // Отклонённая анкета в каталоге быть не должна; если флаг остался
      // от прошлого — это ошибка данных, и повторять её на экране незачем.
      showInCatalog: false,
    };
  }

  if (status === 'APPROVED') {
    const hasPrice = typeof app.price === 'number' && app.price > 0;
    return {
      labelKey: 'casting.status.approved',
      tone: 'purchased',
      price: hasPrice ? app.price : null,
      noteKey: app.paid ? 'casting.status.paid' : 'casting.status.paymentSoon',
      showInCatalog: app.isWebShow === true,
    };
  }

  return {
    labelKey: 'casting.status.pending',
    tone: 'info',
    price: null,
    noteKey: 'casting.status.pendingNote',
    // Админ включает витрину независимо от статуса (как в старой админке),
    // поэтому флаг показываем как есть.
    showInCatalog: app.isWebShow === true,
  };
}

/**
 * `2026-09-19T12:34:56` → `19.09.2026`.
 *
 * Вручную, а не `toLocaleDateString`: поддержка `Intl` в Hermes зависит
 * от сборки (та же причина, что в `lib/money`). Бэкенд отдаёт
 * `LocalDateTime` без зоны — это время сервера, и переводить его в
 * «местное» через `new Date()` значило бы сдвинуть дату у полуночи.
 */
export function formatDate(iso: string | null | undefined): string {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso ?? '');
  return m ? `${m[3]}.${m[2]}.${m[1]}` : '';
}

/**
 * Какую заявку показать в карточке на вкладке «Casting».
 *
 * Сначала открытая (на рассмотрении), затем самая свежая: человеку
 * важнее «что сейчас ждёт ответа», чем старый отказ.
 */
export function pickHeadline(apps: MyApplication[]): MyApplication | null {
  if (apps.length === 0) return null;
  const pending = apps.find((a) => normalizeStatus(a.status) === 'PENDING');
  if (pending) return pending;
  return [...apps].sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))[0] ?? null;
}
