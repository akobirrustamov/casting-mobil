import { useState } from 'react';
import { adminApi } from '../api/client';
import ConfirmDialog, { useConfirm } from '../components/ConfirmDialog';
import { usePanelI18n } from '../i18n';
import { count } from '../utils/format';

const SCOPES = ['APP_USERS', 'STAFF', 'ALL'];

/**
 * Ommaviy majburiy chiqarish — faqat SUPER_ADMIN va undan yuqori.
 *
 * Hodisa paytidagi chora (token sizib chiqdi, hisob buzildi): tanlangan
 * guruhdagi hammani barcha qurilmalardan chiqaradi, ular qayta kirishi
 * kerak bo'ladi. Amalni bajargan admin va o'zidan yuqori/teng roldagi
 * xodimlar chiqarilmaydi — bu backend qoidasi (`SessionAdminService`).
 *
 * ⚠️ Tasdiqlash so'zi so'raladi: xato bosish butun auditoriyani kirish
 * oynasiga qaytaradi, oddiy «Ha» buning uchun juda arzon.
 */
export default function SessionsCard() {
  const { t } = usePanelI18n();
  const [scope, setScope] = useState('APP_USERS');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const confirmer = useConfirm();

  const confirmWord = t('ss.confirmWord');

  const ask = () => {
    setResult(null);
    setError(null);
    confirmer.ask({
      title: t('ss.confirmTitle'),
      message: t(`ss.scope.${scope}`) + ' — ' + t('ss.confirmMessage'),
      note: t('ss.note'),
      confirmLabel: t('ss.action'),
      input: { label: t('ss.confirmInput', { word: confirmWord }), required: true },
      run: async (typed) => {
        if (String(typed || '').trim().toUpperCase() !== confirmWord.toUpperCase()) {
          setError({ message: t('ss.confirmMismatch', { word: confirmWord }) });
          return;
        }
        try {
          setResult(await adminApi.logoutAllSessions(scope));
        } catch (err) {
          setError(err);
        }
      },
    });
  };

  return (
    <div className="uz-card p-5 mt-6">
      <div className="uz-h2 mb-1" style={{ fontSize: 15 }}>{t('ss.title')}</div>
      <p className="uz-muted text-sm mb-4">{t('ss.hint')}</p>

      <div className="flex flex-wrap gap-4 mb-4">
        {SCOPES.map((s) => (
          <label key={s} className="flex items-center gap-2 text-sm" style={{ cursor: 'pointer' }}>
            <input type="radio" name="session-scope" value={s}
                   checked={scope === s} onChange={() => setScope(s)} />
            {t(`ss.scope.${s}`)}
          </label>
        ))}
      </div>

      <button type="button" className="uz-btn uz-btn-danger" onClick={ask}>
        {t('ss.action')}
      </button>

      {result && (
        <div role="status" className="mt-4 text-sm" style={{ color: 'var(--p-success)' }}>
          {t('ss.done', { users: count(result.users), sessions: count(result.sessions) })}
        </div>
      )}
      {error && (
        <div role="alert" className="mt-4 text-sm" style={{ color: 'var(--p-danger)' }}>
          {error.message}
        </div>
      )}

      <ConfirmDialog {...confirmer.props} />
    </div>
  );
}
