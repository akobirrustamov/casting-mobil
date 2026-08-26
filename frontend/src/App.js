import React, { useEffect } from "react";
import { Route, Routes, useLocation, useNavigate } from "react-router-dom";
import ApiCall from "./config/index"

// my pages
import Home from "./pages/home/Home"
import PageNotFound from "./pages/404/404";
import AdminHome from "./admin/admin/AdminHome";
import AdminNews from "./admin/admin/AdminNews";
import DataForm from "./pages/dataForm/DataForm";
import History from "./pages/history/History";
import Appeal from "./pages/appeal/Appeal";
import CastingUser from "./admin/admin/CastingUser";
import CastingUserDetail from "./admin/admin/CastingUserDetail";
import CastingUserAccepted from "./admin/admin/CastingUserAccepted";
import Models from "./pages/models/Models";
import LoginPage from "./admin/LoginAdmin"
import BotHome from "./pages/HomeBot/BotHome"

import PanelApp from "./adminpanel/PanelApp";

import BotAdminHome from "./bot-admin/admin/AdminHome";
import BotAdminNews from "./bot-admin/admin/AdminNews";
import BotCastingUser from "./bot-admin/admin/CastingUser";
import BotCastingUserDetail from "./bot-admin/admin/CastingUserDetail";
import BotCastingUserAccepted from "./bot-admin/admin/CastingUserAccepted";

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
      <Routes>
        {/*  app admin */}
        <Route path={"/aadmin/login"} element={<LoginPage />} />
        <Route path={"/aadmin/casting-users/web"} element={<CastingUser />} />
        <Route path={"/aadmin/casting-users/:castingUserId"} element={<CastingUserDetail />} />
        {/* UZCASTING admin paneli - o'z ichida marshrutlanadi */}
        <Route path={"/app/panel/*"} element={<PanelApp />} />
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
    </div >
  );
}

export default App;
