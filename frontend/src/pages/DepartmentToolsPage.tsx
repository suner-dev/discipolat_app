import { Navigate, useParams } from 'react-router-dom';

/**
 * Redirection — les outils du département sont désormais intégrés dans
 * l'onglet « Gestion » (`/departments/:id/manage`). Cette page assure
 * la rétrocompatibilité des anciens liens/bookmarks.
 */
export default function DepartmentToolsPage() {
  const { id } = useParams<{ id: string }>();
  return <Navigate to={`/departments/${id}/manage`} replace />;
}
