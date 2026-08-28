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
import HelpPage from './pages/HelpPage';
import ContentPage from './pages/ContentPage';
import CreatorsPage from './pages/CreatorsPage';
import TaxonomyPage from './pages/TaxonomyPage';
import MediaPage from './pages/MediaPage';
import HomepagePage from './pages/HomepagePage';
import BannerPage from './pages/BannerPage';
import CommentsPage from './pages/CommentsPage';
import NotificationsPage from './pages/NotificationsPage';
import UsersPage from './pages/UsersPage';
import UserDetailPage from './pages/UserDetailPage';
import DonationsPage from './pages/DonationsPage';
import CastingPage from './pages/CastingPage';
import SubscriptionsPage from './pages/SubscriptionsPage';
import TariffsPage from './pages/TariffsPage';
import SettingsPage from './pages/SettingsPage';
import ReportsPage from './pages/ReportsPage';
import AuditPage from './pages/AuditPage';
import StaffPage from './pages/staff/StaffPage';

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
            {/* ⚠️ Ruxsat TALAB QILINMAYDI. Yo'riqnoma xodimga o'zi
                nima qila olishini aytadi — uni ko'rish uchun alohida
                ruxsat so'rash mantiqsiz bo'lardi. Sahifaning o'zi
                mazmunni ruxsatga qarab filtrlaydi. */}
            <Route path="help" element={<HelpPage />} />
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
                  {/* ⚠️ `key` SHART. Ikkala marshrut ham bir xil
                      komponentni bir joyda chizadi, ya'ni React uni
                      qayta ishlatadi va HOLATNI SAQLAB QOLADI. Natijada
                      kategoriyani tahrirlash oynasi ochiq turganda
                      «Janrlar» ga o'tilsa, oyna yopilmasdi va saqlash
                      kategoriya ma'lumotini o'sha raqamli JANR ustiga
                      yozardi. Qidiruv matni va sahifa raqami ham
                      eskisidan qolib ketardi. */}
                  <TaxonomyPage key="category" kind="category" />
                </RequirePermission>
              }
            />
            <Route
              path="genres"
              element={
                <RequirePermission permission="GENRE_VIEW">
                  <TaxonomyPage key="genre" kind="genre" />
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
                  <BannerPage key="ad" kind="ad" />
                </RequirePermission>
              }
            />
            <Route
              path="premieres"
              element={
                <RequirePermission permission="PREMIERE_VIEW">
                  <BannerPage key="premiere" kind="premiere" />
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
            {/* ⚠️ Bitta foydalanuvchi sahifasi (F6). Ro'yxatdan
                ALOHIDA marshrut: balans, qurilmalar va obuna tarixi
                birgalikda kerak bo'lganda modal oynalar orasida yurish
                kontekstni yo'qotardi. */}
            <Route
              path="users/:userId"
              element={
                <RequirePermission permission="USER_VIEW">
                  <UserDetailPage />
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
              path="subscriptions"
              element={
                <RequirePermission permission="SUBSCRIPTION_VIEW">
                  <SubscriptionsPage />
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
