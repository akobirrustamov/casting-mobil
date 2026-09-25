import React, { Suspense, lazy, useEffect } from "react";
import { Route, Routes, useLocation, useNavigate } from "react-router-dom";
import ApiCall from "./config/index"

// Bosh sahifa darhol yuklanadi — foydalanuvchilarning aksariyati shu yerga
// keladi. Qolgan sahifalar (admin panel, bot, tomosha) alohida bo'laklarga
// ajratilgan: ilgari hammasi bitta 2.2 MB main.js ichida edi va bosh sahifa
// ham recharts, primereact, hls.js va boshqalarni yuklab olishga majbur edi.
import Home from "./pages/home/Home"

const PageNotFound = lazy(() => import("./pages/404/404"));
const DataForm = lazy(() => import("./pages/dataForm/DataForm"));
const History = lazy(() => import("./pages/history/History"));
const Appeal = lazy(() => import("./pages/appeal/Appeal"));
const CastingUser = lazy(() => import("./admin/admin/CastingUser"));
const CastingUserDetail = lazy(() => import("./admin/admin/CastingUserDetail"));
const Models = lazy(() => import("./pages/models/Models"));
const LoginPage = lazy(() => import("./admin/LoginAdmin"));
const BotHome = lazy(() => import("./pages/HomeBot/BotHome"));

const PanelApp = lazy(() => import("./adminpanel/PanelApp"));
const ViewerLayout = lazy(() => import("./viewer/ViewerLayout"));
const ViewerSignIn = lazy(() => import("./viewer/pages/SignInPage"));
const ViewerWatch = lazy(() => import("./viewer/pages/WatchPage"));

const BotAdminHome = lazy(() => import("./bot-admin/admin/AdminHome"));
const BotAdminNews = lazy(() => import("./bot-admin/admin/AdminNews"));
const BotCastingUser = lazy(() => import("./bot-admin/admin/CastingUser"));
const BotCastingUserDetail = lazy(() => import("./bot-admin/admin/CastingUserDetail"));
const BotCastingUserAccepted = lazy(() => import("./bot-admin/admin/CastingUserAccepted"));

// ⚠️ Ilgari App() ichida ["/dashboard"] turgan, lekin bunday marshrut loyihada
// YO'Q — ya'ni tekshiruv hech qachon ishlamagan va admin sahifalari umuman
// qo'riqlanmagan. Haqiqiy admin manzillari quyida.
//
// Komponentdan TASHQARIDA: aks holda har renderda yangi massiv yaratilib,
// useEffect bog'liqligi doim o'zgargandek ko'rinardi.
const BLOCKED_PAGES = [
  "/aadmin",   // sayt admini
  "/admin"     // Telegram bot admini
];

// /aadmin/login ATAYLAB qo'riqlanmaydi: aks holda login sahifasi o'zini
// o'ziga yo'naltirib, cheksiz sikl hosil bo'lardi.
const PUBLIC_ADMIN_PAGES = [
  "/aadmin/login"
];

// Eski panelga kirishi mumkin bo'lgan rollar.
// WORKER ataylab YO'Q: u yangi /app/panel uchun, eski panelda unga mos ekran yo'q.
const ALLOWED_LEGACY_ROLES = [
  "ROLE_ADMIN",
  "ROLE_SUPERADMIN",
  "ROLE_GIPERSUPERADMIN"
];

function App() {

  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    checkSecurity();
    // checkSecurity faqat manzil va navigate'ga bog'liq — ular o'zgarganda
    // qayta ishga tushadi. Funksiyaning o'zini bog'liqlikka qo'shish uni
    // har renderda qayta yaratishga majbur qilardi.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.pathname, navigate]);
  async function checkSecurity() {
    const path = location.pathname;

    const isPublic = PUBLIC_ADMIN_PAGES.some((p) => path.startsWith(p));
    const isProtected = BLOCKED_PAGES.some((p) => path.startsWith(p));
    if (isPublic || !isProtected) {
      return;
    }

    const accessToken = localStorage.getItem("access_token");
    if (!accessToken) {
      navigate("/aadmin/login");
      return;
    }

    try {
      const res = await ApiCall("/api/v1/security", "GET");

      if (res?.data === 401 || res?.error) {
        navigate("/aadmin/login");
        return;
      }

      // ⚠️ Ilgari rol tekshiruvi `res?.error` ichida turgan, ya'ni FAQAT xato
      // bo'lganda ishlagan — muvaffaqiyatli javobda hech qachon bajarilmagan.
      // Endi to'g'ri: rol muvaffaqiyatli javobda tekshiriladi.
      const roles = Array.isArray(res?.data) ? res.data : [];
      const allowed = roles.some((r) => ALLOWED_LEGACY_ROLES.includes(r?.name));
      if (!allowed) {
        navigate("/404");
      }
    } catch (e) {
      // Token yaroqsiz, muddati o'tgan yoki server javob bermadi
      navigate("/aadmin/login");
    }
  }

  return (
    <div>
      <Suspense fallback={<div className="min-h-screen bg-[#0b1220]" />}>
      <Routes>
        {/*  app admin */}
        <Route path={"/aadmin/login"} element={<LoginPage />} />
        <Route path={"/aadmin/casting-users/web"} element={<CastingUser />} />
        <Route path={"/aadmin/casting-users/:castingUserId"} element={<CastingUserDetail />} />
        {/* UZCASTING admin paneli - o'z ichida marshrutlanadi */}
        <Route path={"/app/panel/*"} element={<PanelApp />} />

        {/* UZCASTING tomoshabin yuzasi — video ko'rish.
            Yo'lsiz marshrut faqat qobiq beradi (tarjima va palitra),
            manzillarga tegmaydi — shuning uchun ular qisqa. */}
        <Route element={<ViewerLayout />}>
          <Route path={"/kirish"} element={<ViewerSignIn />} />
          <Route path={"/tomosha/:type/:id"} element={<ViewerWatch />} />
        </Route>
        <Route path={"/*"} element={<PageNotFound />} />
        <Route path={"/"} element={<Home />} />

        {/*bot admin*/}
        <Route path={"/admin/home"} element={<BotAdminHome />} />
        <Route path={"/admin/news"} element={<BotAdminNews />} />
        <Route path={"/admin/casting-users"} element={<BotCastingUser />} />
        <Route path={"/admin/accepted"} element={<BotCastingUserAccepted />} />
        <Route path={"/admin/casting-users/:castingUserId"} element={<BotCastingUserDetail />} />


        {/*  app user */}

        <Route path={"/bot/:userId"} element={<BotHome />} />
        <Route path={"/data-form/:userId"} element={<DataForm />} />
        <Route path={"/history/:userId"} element={<History />} />
        <Route path={"/appeal/:userId"} element={<Appeal />} />
        <Route path={"/models"} element={<Models />} />
      </Routes>
      </Suspense>
    </div >
  );
}

export default App;
