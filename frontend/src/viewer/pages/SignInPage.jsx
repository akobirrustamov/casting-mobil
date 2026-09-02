import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { signIn } from '../api/client';
import { useViewerI18n } from '../i18n';

/**
 * Tomoshabin uchun kirish — `/kirish`.
 *
 * <h2>⚠️ Faqat KIRISH, ro'yxatdan o'tish yo'q</h2>
 * Ro'yxatdan o'tish SMS kodni talab qiladi
 * ({@code /app/auth/register/start} → {@code /confirm} →
 * {@code /complete}) va uchta ekranli alohida oqim. Bu yerda uni
 * yarim holda qo'yish odamni «kod keldi, keyin nima?» degan holatga
 * tashlardi. Web hozircha mobil ilovada ro'yxatdan o'tganlar uchun.
 *
 * <h2>⚠️ Parol bilan, SMS bilan emas</h2>
 * {@code /app/auth/login} telefon va parolni oladi. OTP orqali
 * kirish ham bor, lekin u SMS yuboradi — har sinov uchun haqiqiy
 * xabar va haqiqiy pul.
 */
export default function SignInPage() {
  const { t } = useViewerI18n();
  const navigate = useNavigate();
  const [params] = useSearchParams();

  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError(null);

    try {
      await signIn(phone.trim(), password);

      // ⚠️ Qaytish manzili PARAMETRDAN, lekin faqat ichki yo'l.
      // Tashqi manzil qabul qilinsa, havola bilan odamni begona
      // saytga olib chiqish mumkin bo'lardi — kirishdan darhol
      // keyin, ya'ni eng ishonchli lahzada.
      const next = params.get('next');
      navigate(next && next.startsWith('/') && !next.startsWith('//') ? next : '/');
    } catch (err) {
      const status = err?.response?.status;
      setError(status === 401 || status === 422 ? t('error.credentials') : t('error.network'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="uz-viewer">
      <form className="uz-viewer-signin" onSubmit={submit}>
        <h1 className="uz-viewer-title">{t('signIn.title')}</h1>

        <label className="uz-field">
          <span>{t('signIn.phone')}</span>
          <input
            type="tel"
            inputMode="tel"
            autoComplete="username"
            placeholder="+998 90 123 45 67"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            required
          />
        </label>

        <label className="uz-field">
          <span>{t('signIn.password')}</span>
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>

        {error && <div className="uz-alert uz-alert-danger">{error}</div>}

        <button type="submit" className="uz-btn uz-btn-primary" disabled={busy}>
          {busy ? t('signIn.loading') : t('signIn.submit')}
        </button>

        <p className="uz-muted">{t('signIn.hint')}</p>
      </form>
    </div>
  );
}
