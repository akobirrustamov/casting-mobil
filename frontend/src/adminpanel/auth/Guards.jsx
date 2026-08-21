import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { ForbiddenState } from '../components/States';
import { LoadingState } from '../components/States';

/** Kirmagan bo'lsa login sahifasiga. */
export function RequireAuth({ children }) {
  const { isAuthed, restoring } = useAuth();
  const location = useLocation();

  // Profil tiklanayotganda login sahifasiga otib yubormaymiz -
  // aks holda har yangilanishda chiqib ketgandek ko'rinadi.
  if (restoring) {
    return (
      <div className="uz-panel p-8">
        <LoadingState rows={3} />
      </div>
    );
  }
  if (!isAuthed) {
    return <Navigate to="/app/panel/login" replace state={{ from: location.pathname }} />;
  }
  return children;
}

/**
 * Ruxsat bo'lmasa 403 ko'rsatadi.
 * Bu xavfsizlik emas - backend baribir tekshiradi.
 */
export function RequirePermission({ permission, role, children }) {
  const { can, atLeast } = useAuth();
  const ok = permission ? can(permission) : role ? atLeast(role) : true;
  return ok ? children : <ForbiddenState />;
}
