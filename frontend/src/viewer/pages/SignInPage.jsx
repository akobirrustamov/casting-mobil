import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { completeSignUp, sendCode, verifyCode } from '../api/client';
import { useViewerI18n } from '../i18n';

/**
 * Tomoshabin uchun kirish — `/kirish`.
 *
 * <h2>⚠️ Nega parol emas, SMS kod</h2>
 * Bu sahifa ilgari {@code POST /app/auth/login} ga telefon va parol
 * yuborardi. O'sha endpoint backenddan OLIB TASHLANGAN — butun
 * kirish OTP ga o'tkazilgan, sayt tomoni esa yangilanmagan.
 *
 * Natijada saytdagi kirish umuman ishlamasdi, va nosozlik jimgina
 * edi: mavjud bo'lmagan yo'l ham 401 qaytaradi, ya'ni ekranda
 * «parol xato» chiqardi. Odam parolini qayta-qayta terib ko'rardi.
 *
 * ⚠️ Parolni qaytarishning ma'nosi ham yo'q edi: ilova
 * foydalanuvchisi parol O'RNATA olmaydi — bunday oqim mavjud emas.
 *
 * <h2>Uch qadam</h2>
 * <pre>
 *   raqam  → kod yuboriladi
 *   kod    → eski foydalanuvchi KIRADI, yangisi keyingi qadamga o'tadi
 *   ism    → faqat birinchi marta
 * </pre>
 */
export default function SignInPage() {
  const { t } = useViewerI18n();
  const navigate = useNavigate();
  const [params] = useSearchParams();

  const [step, setStep] = useState('phone');
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [seconds, setSeconds] = useState(0);

  /**
   * Qayta yuborishgacha qolgan vaqt.
   *
   * ⚠️ Sanoq YAKKA bo'lishi shart. Har bir «kod olish» bosilganda
   * yangi interval ochilsa, ular birga sanab hisobni bir necha
   * barobar tez tushirardi va tugma vaqtidan oldin yonardi.
   */
  const timer = useRef(null);

  const startCountdown = useCallback((total) => {
    if (timer.current) clearInterval(timer.current);
    setSeconds(total);
    if (total <= 0) return;

    timer.current = setInterval(() => {
      setSeconds((left) => {
        if (left <= 1) {
          clearInterval(timer.current);
          timer.current = null;
          return 0;
        }
        return left - 1;
      });
    }, 1000);
  }, []);

  // ⚠️ Sahifadan chiqilganda interval to'xtatilishi shart: aks holda
  // u yo'q komponentning holatini yangilashda davom etardi.
  useEffect(() => () => {
    if (timer.current) clearInterval(timer.current);
  }, []);

  /**
   * Server xatosini odam tushunadigan matnga aylantiradi.
   *
   * ⚠️ Ilgari 401 va 422 dan boshqa hamma narsa «Internetni
   * tekshiring» bo'lardi. Cheklovga urilgan odam ham shu xabarni
   * ko'rardi va internetini qayta-qayta tekshirardi — muammo esa
   * butunlay boshqa joyda edi.
   */
  const describe = (err, whenInvalid) => {
    const status = err?.response?.status;
    if (status === 429) return t('error.tooMany');
    if (status === 401 || status === 422 || status === 400) return whenInvalid;
    return t('error.network');
  };

  const submitPhone = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const { expiresInSeconds } = await sendCode(phone.trim());
      setStep('code');
      setCode('');
      startCountdown(expiresInSeconds);
    } catch (err) {
      setError(describe(err, t('error.phone')));
    } finally {
      setBusy(false);
    }
  };

  const submitCode = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const { nameRequired } = await verifyCode(phone.trim(), code.trim());
      if (nameRequired) {
        setStep('name');
        return;
      }
      done();
    } catch (err) {
      setError(describe(err, t('error.credentials')));
    } finally {
      setBusy(false);
    }
  };

  const submitName = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await completeSignUp(phone.trim(), name.trim());
      done();
    } catch (err) {
      setError(describe(err, t('error.credentials')));
    } finally {
      setBusy(false);
    }
  };

  /**
   * ⚠️ Qaytish manzili PARAMETRDAN, lekin faqat ichki yo'l.
   *
   * Tashqi manzil qabul qilinsa, havola bilan odamni begona saytga
   * olib chiqish mumkin bo'lardi — kirishdan darhol keyin, ya'ni eng
   * ishonchli lahzada.
   */
  function done() {
    const next = params.get('next');
    navigate(next && next.startsWith('/') && !next.startsWith('//') ? next : '/');
  }

  const backToPhone = () => {
    if (timer.current) clearInterval(timer.current);
    timer.current = null;
    setSeconds(0);
    setError(null);
    setCode('');
    setStep('phone');
  };

  const alert = error && <div className="uz-alert uz-alert-danger">{error}</div>;

  // ------------------------------------------------------------ 1. raqam
  if (step === 'phone') {
    return (
      <div className="uz-viewer">
        <form className="uz-viewer-signin" onSubmit={submitPhone}>
          <h1 className="uz-viewer-title">{t('signIn.title')}</h1>

          <label className="uz-field">
            <span>{t('signIn.phone')}</span>
            <input
              type="tel"
              inputMode="tel"
              autoComplete="tel"
              placeholder="+998 90 123 45 67"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              required
            />
          </label>

          {alert}

          <button type="submit" className="uz-btn uz-btn-primary" disabled={busy}>
            {busy ? t('signIn.loading') : t('signIn.submit')}
          </button>

          <p className="uz-muted">{t('signIn.hint')}</p>
        </form>
      </div>
    );
  }

  // -------------------------------------------------------------- 2. kod
  if (step === 'code') {
    return (
      <div className="uz-viewer">
        <form className="uz-viewer-signin" onSubmit={submitCode}>
          <h1 className="uz-viewer-title">{t('signIn.codeTitle')}</h1>
          <p className="uz-muted">{t('signIn.codeSent', { phone: phone.trim() })}</p>

          <label className="uz-field">
            <span>{t('signIn.code')}</span>
            {/*
              ⚠️ `type="text"` va `inputMode="numeric"`, `type="number"` EMAS.
              Raqamli maydon boshidagi nolni yeb qo'yadi va g'ildirak
              bilan qiymatni beixtiyor o'zgartirib yuborish mumkin.
            */}
            <input
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              required
              autoFocus
            />
          </label>

          {alert}

          <button type="submit" className="uz-btn uz-btn-primary" disabled={busy}>
            {busy ? t('signIn.codeLoading') : t('signIn.codeSubmit')}
          </button>

          {/*
            ⚠️ Qayta yuborish sanoq tugagunicha O'CHIQ: har bosish
            haqiqiy SMS va haqiqiy pul. Ochiq tugma sabrsiz odamga
            uni ketma-ket bosish imkonini berardi.
          */}
          <button
            type="button"
            className="uz-btn uz-btn-ghost"
            onClick={submitPhone}
            disabled={busy || seconds > 0}
          >
            {seconds > 0 ? t('signIn.resendIn', { seconds }) : t('signIn.resend')}
          </button>

          <button type="button" className="uz-btn uz-btn-ghost" onClick={backToPhone}>
            {t('signIn.back')}
          </button>
        </form>
      </div>
    );
  }

  // -------------------------------------------------------------- 3. ism
  return (
    <div className="uz-viewer">
      <form className="uz-viewer-signin" onSubmit={submitName}>
        <h1 className="uz-viewer-title">{t('signIn.nameTitle')}</h1>
        <p className="uz-muted">{t('signIn.nameHint')}</p>

        <label className="uz-field">
          <span>{t('signIn.name')}</span>
          <input
            type="text"
            autoComplete="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            autoFocus
          />
        </label>

        {alert}

        <button type="submit" className="uz-btn uz-btn-primary" disabled={busy}>
          {busy ? t('signIn.codeLoading') : t('signIn.nameSubmit')}
        </button>
      </form>
    </div>
  );
}
