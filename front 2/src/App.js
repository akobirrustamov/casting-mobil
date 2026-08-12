import React, { useEffect } from "react";
import './App.css';
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

import BotAdminHome from "./bot-admin/admin/AdminHome";
import BotAdminNews from "./bot-admin/admin/AdminNews";
import BotCastingUser from "./bot-admin/admin/CastingUser";
import BotCastingUserDetail from "./bot-admin/admin/CastingUserDetail";
import BotCastingUserAccepted from "./bot-admin/admin/CastingUserAccepted";

function App() {

  const blockedPages = [
    "/dashboard"
  ];
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {

    checkSecurity()
  }, [blockedPages, location.pathname, navigate])
  async function checkSecurity() {
    if (blockedPages.some((blockedPage) => location.pathname.startsWith(blockedPage))) {
      let accessToken = localStorage.getItem("access_token");
      const res = await ApiCall("/api/v1/security", "GET")
      if (res?.data == 401) {
        navigate("/aadmin/login");
      }
      if (accessToken !== null) {
        if (res?.data !== 401 && res?.error) {
          if (res?.data[0]?.name !== "ROLE_ADMIN") {
            navigate("/404")
          }
        }
      } else {
        navigate("/aadmin/login");
      }
    }
  }

  return (
    <div>
      <Routes>
        {/*  app admin */}
        <Route path={"/aadmin/login"} element={<LoginPage />} />
        <Route path={"/aadmin/casting-users/web"} element={<CastingUser />} />
        <Route path={"/aadmin/casting-users/:castingUserId"} element={<CastingUserDetail />} />

        <Route path={"/*"} element={<PageNotFound />} />
        <Route path={"/"} element={<Home />} />b

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
