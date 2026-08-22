/**
 * UZCASTING admin panelining kirish nuqtasi.
 *
 * Mavjud sayt va bot admini ({@code src/admin}, {@code src/bot-admin}) ga
 * TEGILMAYDI - bu butunlay alohida bo'lim, /app/panel/* manzilida.
 * Yangi platformaning barcha yo'llari /app/** makonida turadi.
 */
import { Route, Routes } from 'react-router-dom';

import './theme/panel.css';
import { PanelI18nProvider } from './i18n';
import { AuthProvider } from './auth/AuthContext';
import { RequireAuth, RequirePermission } from './auth/Guards';
import AdminLayout from './layout/AdminLayout';

import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ContentPage from './pages/ContentPage';
import CreatorsPage from './pages/CreatorsPage';
import TaxonomyPage from './pages/TaxonomyPage';
import MediaPage from './pages/MediaPage';
import HomepagePage from './pages/HomepagePage';
import BannerPage from './pages/BannerPage';
import CommentsPage from './pages/CommentsPage';
import NotificationsPage from './pages/NotificationsPage';
import UsersPage from './pages/UsersPage';
import DonationsPage from './pages/DonationsPage';
import CastingPage from './pages/CastingPage';
import TariffsPage from './pages/TariffsPage';
import SettingsPage from './pages/SettingsPage';
import ReportsPage from './pages/ReportsPage';
import AuditPage from './pages/AuditPage';
import StaffPage from './pages/StaffPage';

export default function PanelApp() {
  return (
    <PanelI18nProvider>
      <AuthProvider>
        <Routes>
          <Route path="login" element={<LoginPage />} />

          <Route
            element={
              <RequireAuth>
                <AdminLayout />
              </RequireAuth>
            }
          >
            <Route index element={<DashboardPage />} />
            <Route
              path="content"
              element={
                <RequirePermission permission="CONTENT_VIEW">
                  <ContentPage />
                </RequirePermission>
              }
            />
            <Route
              path="creators"
              element={
                <RequirePermission permission="CREATOR_VIEW">
                  <CreatorsPage />
                </RequirePermission>
              }
            />
            <Route
              path="categories"
              element={
                <RequirePermission permission="CATEGORY_VIEW">
                  <TaxonomyPage kind="category" />
                </RequirePermission>
              }
            />
            <Route
              path="genres"
              element={
                <RequirePermission permission="GENRE_VIEW">
                  <TaxonomyPage kind="genre" />
                </RequirePermission>
              }
            />
            <Route
              path="media"
              element={
                <RequirePermission permission="MEDIA_VIEW">
                  <MediaPage />
                </RequirePermission>
              }
            />
            <Route
              path="homepage"
              element={
                <RequirePermission permission="HOMEPAGE_VIEW">
                  <HomepagePage />
                </RequirePermission>
              }
            />
            <Route
              path="ads"
              element={
                <RequirePermission permission="ADVERTISEMENT_VIEW">
                  <BannerPage kind="ad" />
                </RequirePermission>
              }
            />
            <Route
              path="premieres"
              element={
                <RequirePermission permission="PREMIERE_VIEW">
                  <BannerPage kind="premiere" />
                </RequirePermission>
              }
            />
            <Route
              path="comments"
              element={
                <RequirePermission permission="COMMENT_VIEW">
                  <CommentsPage />
                </RequirePermission>
              }
            />
            <Route
              path="notifications"
              element={
                <RequirePermission permission="NOTIFICATION_VIEW">
                  <NotificationsPage />
                </RequirePermission>
              }
            />
            <Route
              path="users"
              element={
                <RequirePermission permission="USER_VIEW">
                  <UsersPage />
                </RequirePermission>
              }
            />
            <Route
              path="donations"
              element={
                <RequirePermission permission="DONATION_VIEW">
                  <DonationsPage />
                </RequirePermission>
              }
            />
            {/*
              Eski casting moduli (ТЗ §49). Ruxsat CONTENT_VIEW: eski
              tizimda alohida ruxsat yo'q va uni qo'shish eski kodga
              tegishni talab qilardi — buyurtmachi buni taqiqlagan.
            */}
            <Route
              path="casting"
              element={
                <RequirePermission permission="CONTENT_VIEW">
                  <CastingPage />
                </RequirePermission>
              }
            />
            <Route
              path="tariffs"
              element={
                <RequirePermission permission="TARIFF_VIEW">
                  <TariffsPage />
                </RequirePermission>
              }
            />
            <Route
              path="settings"
              element={
                <RequirePermission permission="SETTINGS_VIEW">
                  <SettingsPage />
                </RequirePermission>
              }
            />
            <Route
              path="reports"
              element={
                <RequirePermission permission="REPORT_VIEW">
                  <ReportsPage />
                </RequirePermission>
              }
            />
            <Route
              path="audit"
              element={
                <RequirePermission role="ADMIN">
                  <AuditPage />
                </RequirePermission>
              }
            />
            <Route
              path="staff"
              element={
                <RequirePermission role="ADMIN">
                  <StaffPage />
                </RequirePermission>
              }
            />
          </Route>
        </Routes>
      </AuthProvider>
    </PanelI18nProvider>
  );
}
