/**
 * Kim kirgan, qanday rol va ruxsatlari bor.
 *
 * ⚠️ Bu yerdagi tekshiruvlar faqat INTERFEYS uchun: menyuni yashirish va
 * tugmalarni o'chirish. Haqiqiy himoya backendda - u baribir 403 qaytaradi (§11).
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { adminApi, setUnauthorizedHandler, tokenStore } from '../api/client';

const AuthContext = createContext(null);

/** Rol ierarxiyasi - backenddagi PlatformRole bilan bir xil. */
const ROLE_LEVEL = {
  HYPER_ADMIN: 100,
  SUPER_ADMIN: 80,
  ADMIN: 60,
  WORKER: 40,
  USER: 10,
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => tokenStore.getUser());
  // ⚠️ Access token endi xotirada — sahifa yangilanishida u YO'Q (§61).
  // Shuning uchun tiklash tokenning borligiga emas, saqlangan profilga
  // qarab boshlanadi: refresh cookie'si bo'lsa sessiya tiklanadi.
  const [restoring, setRestoring] = useState(Boolean(tokenStore.getUser()));

  const signOut = useCallback(() => {
    // Serverga ham aytamiz: aks holda refresh token muddati tugaguncha
    // amal qilaverardi va «chiqish» faqat ko'rinish bo'lardi.
    adminApi.logout().catch(() => {
      /* tarmoq yo'q bo'lsa ham klient tomonda chiqamiz */
    });
    tokenStore.clear();
    setUser(null);
  }, []);

  // 401 kelganda - token eskirgan, chiqaramiz
  useEffect(() => {
    setUnauthorizedHandler(() => {
      tokenStore.clear();
      setUser(null);
    });
  }, []);

  // Sahifa yangilanganda profilni serverdan tiklaymiz: rol yoki ruxsat
  // o'zgargan bo'lishi mumkin, localStorage'dagi nusxaga ishonib bo'lmaydi.
  useEffect(() => {
    if (!tokenStore.getUser()) {
      setRestoring(false);
      return;
    }
    let cancelled = false;
    // Avval access tokenni cookie orqali tiklaymiz, keyin profilni
    // serverdan olamiz: rol yoki ruxsat o'zgargan bo'lishi mumkin.
    adminApi
      .refreshSession()
      .then(() => adminApi.me())
      .then((fresh) => {
        if (cancelled) return;
        tokenStore.setUser(fresh);
        setUser(fresh);
      })
      .catch(() => {
        if (!cancelled) signOut();
      })
      .finally(() => {
        if (!cancelled) setRestoring(false);
      });
    return () => {
      cancelled = true;
    };
  }, [signOut]);

  const signIn = useCallback(async (phone, password) => {
    const res = await adminApi.login(phone, password);
    tokenStore.set(res.accessToken);
    tokenStore.setUser(res.user);
    setUser(res.user);
    return res.user;
  }, []);

  /**
   * Ruxsat bormi.
   * ADMIN va undan yuqori rollarda ruxsatlar ro'yxati ishlatilmaydi -
   * ularda rol darajasida to'liq huquq (backendda ham xuddi shunday).
   */
  const can = useCallback(
    (permission) => {
      if (!user?.role) return false;
      if ((ROLE_LEVEL[user.role] || 0) >= ROLE_LEVEL.ADMIN) return true;
      return (user.permissions || []).includes(permission);
    },
    [user]
  );

  const atLeast = useCallback(
    (role) => (ROLE_LEVEL[user?.role] || 0) >= (ROLE_LEVEL[role] || 0),
    [user]
  );

  const value = useMemo(
    () => ({ user, restoring, signIn, signOut, can, atLeast, isAuthed: Boolean(user) }),
    [user, restoring, signIn, signOut, can, atLeast]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth faqat AuthProvider ichida ishlaydi');
  return ctx;
}
