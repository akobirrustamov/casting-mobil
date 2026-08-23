/**
 * ТЗ §86 — kritik oqimlar: kirish va taqiqlangan sahifa.
 *
 * ⚠️ Bu testlar frontend guardlarini tekshiradi, LEKIN ular
 * xavfsizlik dalili EMAS. Menyu yashirilishi va 403 sahifasi —
 * qulaylik; haqiqiy himoya backendda va u alohida tekshiriladi
 * (`AcceptanceCriteriaTest` §78, 6 va 8-bandlar).
 */
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { RequirePermission, RequireAuth } from '../Guards';
import { PanelI18nProvider } from '../../i18n';

// `AuthContext` eksport qilinmagan va faqat test uchun uni ochish
// noto'g'ri bo'lardi - hookning o'zi almashtiriladi.
const mockAuth = { current: null };
jest.mock('../AuthContext', () => ({
  useAuth: () => mockAuth.current,
}));

function withAuth(value, ui) {
  mockAuth.current = value;
  // `ForbiddenState` tarjima kontekstini talab qiladi - haqiqiy
  // provayder ishlatiladi, tarjimalar ham shu bilan tekshiriladi.
  return render(
    <MemoryRouter initialEntries={['/app/panel/content']}>
      <PanelI18nProvider>
        {/* ⚠️ Maqsad marshruti E'LON QILINISHI shart. Usiz
            `RequireAuth` dagi `Navigate` hech qayerga bora olmay,
            React Router cheksiz qayta chizishga tushadi. */}
        <Routes>
          <Route path="/app/panel/login" element={<div>Kirish sahifasi</div>} />
          <Route path="/app/panel/content" element={ui} />
        </Routes>
      </PanelI18nProvider>
    </MemoryRouter>
  );
}

const worker = (permissions) => ({
  user: { role: 'WORKER', permissions },
  isAuthed: true,
  restoring: false,
  can: (p) => permissions.includes(p),
  atLeast: () => false,
});

describe('Ruxsat qo\'riqchisi', () => {
  test('ruxsati bor xodim sahifani ko\'radi', () => {
    withAuth(worker(['CONTENT_VIEW']),
      <RequirePermission permission="CONTENT_VIEW">
        <div>Kontent ro'yxati</div>
      </RequirePermission>);

    expect(screen.getByText("Kontent ro'yxati")).toBeInTheDocument();
  });

  test('ruxsati yo\'q xodimga 403 ko\'rsatiladi', () => {
    withAuth(worker(['CONTENT_VIEW']),
      <RequirePermission permission="STAFF_VIEW">
        <div>Xodimlar</div>
      </RequirePermission>);

    // Sahifa mazmuni umuman chizilmasligi kerak - shunchaki
    // yashirilgan emas, balki render qilinmagan.
    expect(screen.queryByText('Xodimlar')).not.toBeInTheDocument();
    expect(screen.getByText('🔒')).toBeInTheDocument();
  });

  test('rol darajasi bo\'yicha qo\'riqlash ham ishlaydi', () => {
    // audit va staff sahifalari ruxsat emas, ROL bo'yicha yopilgan.
    withAuth({ ...worker([]), atLeast: (r) => r === 'WORKER' },
      <RequirePermission role="ADMIN">
        <div>Audit jurnali</div>
      </RequirePermission>);

    expect(screen.queryByText('Audit jurnali')).not.toBeInTheDocument();
  });
});

describe('Kirish qo\'riqchisi', () => {
  test('kirmagan foydalanuvchi sahifani ko\'rmaydi', () => {
    withAuth({ isAuthed: false, restoring: false, can: () => false, atLeast: () => false },
      <RequireAuth><div>Maxfiy sahifa</div></RequireAuth>);

    expect(screen.queryByText('Maxfiy sahifa')).not.toBeInTheDocument();
    expect(screen.getByText('Kirish sahifasi')).toBeInTheDocument();
  });

  test('profil tiklanayotganda login sahifasiga otib yuborilmaydi', () => {
    // ⚠️ Access token endi xotirada (§61) - sahifa yangilanganda u
    // yo'q va sessiya cookie orqali tiklanadi. Shu paytda foydalanuvchi
    // "chiqib ketgandek" ko'rinmasligi kerak.
    withAuth({ isAuthed: false, restoring: true, can: () => false, atLeast: () => false },
      <RequireAuth><div>Maxfiy sahifa</div></RequireAuth>);

    expect(screen.queryByText('Maxfiy sahifa')).not.toBeInTheDocument();
    // Login sahifasiga o'tkazilmagan - yuklanish holati ko'rsatilgan.
    expect(screen.queryByText('Kirish sahifasi')).not.toBeInTheDocument();
  });
});
