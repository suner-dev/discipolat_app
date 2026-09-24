import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import api from "@/lib/api";

interface InvitationData {
  email: string;
  role: string;
  tenantName: string;
  organizationName?: string | null;
  scopeType?: string;
  accountExists: boolean;
  expiresAt: string;
}

export default function AcceptInvitationPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token");
  const [invitation, setInvitation] = useState<InvitationData | null>(null);
  const [loading, setLoading] = useState(true);
  const [accepting, setAccepting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    password: "",
    confirmPassword: "",
  });

  useEffect(() => {
    if (!token) {
      setError("Token d'invitation manquant");
      setLoading(false);
      return;
    }
    loadInvitation();
  }, [token]);

  const loadInvitation = async () => {
    if (!token) return;
    try {
      const res = await api.get(`/admin/invitations/validate/${encodeURIComponent(token)}`);
      const data = res.data;
      if (data.email) {
        setInvitation({
          email: data.email,
          role: data.role,
          tenantName: data.tenantName || "Discipolat",
          organizationName: data.organizationName,
          scopeType: data.scopeType,
          accountExists: data.accountExists === true,
          expiresAt: data.expiresAt,
        });
      } else {
        setError(data.message || "Invitation invalide");
      }
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.error || "Invitation invalide ou expirée");
    } finally {
      setLoading(false);
    }
  };

  const handleAccept = async () => {
    if (!token || !invitation) return;
    if (!invitation.accountExists) {
      if (formData.password !== formData.confirmPassword) {
        setError("Les mots de passe ne correspondent pas");
        return;
      }
      if (formData.password.length < 8) {
        setError("Le mot de passe doit contenir au moins 8 caractères");
        return;
      }
    }

    setAccepting(true);
    setError(null);
    try {
      await api.post(`/admin/invitations/accept/${encodeURIComponent(token)}`,
        invitation?.accountExists
          ? {}
          : {
              firstName: formData.firstName,
              lastName: formData.lastName,
              password: formData.password,
            }
      );
      setSuccess(true);
      setTimeout(() => navigate("/login"), 3000);
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.error || "Erreur lors de l'acceptation");
    } finally {
      setAccepting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="spinner h-10 w-10" />
      </div>
    );
  }

  if (error && !invitation) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="max-w-md w-full mx-4 p-8 bg-white rounded-xl shadow-sm border">
          <div className="text-center">
            <svg className="mx-auto h-12 w-12 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <h1 className="mt-4 text-xl font-bold text-gray-900">Invitation invalide</h1>
            <p className="mt-2 text-gray-600">{error}</p>
            <button
              onClick={() => navigate("/login")}
              className="mt-6 w-full px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
            >
              Retour à la connexion
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="max-w-md w-full mx-4 p-8 bg-white rounded-xl shadow-sm border">
          <div className="text-center">
            <svg className="mx-auto h-12 w-12 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <h1 className="mt-4 text-xl font-bold text-gray-900">
              {invitation?.accountExists ? "Invitation acceptée avec succès !" : "Compte créé avec succès !"}
            </h1>
            <p className="mt-2 text-gray-600">Vous êtes maintenant membre de <strong>{invitation?.tenantName}</strong>.</p>
            <p className="mt-2 text-sm text-gray-500">Redirection vers la connexion dans 3 secondes...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
      <div className="max-w-md w-full">
        <div className="bg-white rounded-xl shadow-sm border p-8">
          <div className="text-center mb-8">
            <svg className="mx-auto h-12 w-12 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
            </svg>
            <h1 className="mt-4 text-2xl font-bold text-gray-900">Rejoindre {invitation?.tenantName}</h1>
            <p className="mt-2 text-gray-600">
              {invitation?.accountExists
                ? "Un compte existe déjà. Acceptez l’invitation, puis connectez-vous."
                : <>Vous avez été invité(e) en tant que <strong>{invitation?.role}</strong></>}
            </p>
            {invitation?.organizationName && (
              <p className="mt-1 text-sm text-gray-500">{invitation.organizationName}</p>
            )}
            <p className="mt-1 text-sm text-gray-500">
              Lien expire le {new Date(invitation?.expiresAt || "").toLocaleDateString("fr-FR")}
            </p>
          </div>

          <form onSubmit={(e) => { e.preventDefault(); handleAccept(); }}>
            <div className="space-y-4">
              {!invitation?.accountExists && (
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Prénom</label>
                  <input
                    type="text"
                    required
                    value={formData.firstName}
                    onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    placeholder="Jean"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Nom</label>
                  <input
                    type="text"
                    required
                    value={formData.lastName}
                    onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    placeholder="Dupont"
                  />
                </div>
              </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                <input
                  type="email"
                  value={invitation?.email}
                  readOnly
                  className="w-full px-3 py-2 border rounded-lg bg-gray-50 text-gray-600"
                />
              </div>

              {!invitation?.accountExists && (
              <>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Mot de passe</label>
                <input
                  type="password"
                  required
                  minLength={8}
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  placeholder="••••••••"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Confirmer le mot de passe</label>
                <input
                  type="password"
                  required
                  value={formData.confirmPassword}
                  onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  placeholder="••••••••"
                />
              </div>
              </>
              )}

              {error && (
                <div className="p-3 bg-red-50 text-red-700 rounded-lg text-sm">
                  {error}
                </div>
              )}

              <button
                type="submit"
                disabled={accepting}
                className="w-full py-2.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 font-medium transition-colors"
              >
                {accepting
                  ? "Traitement..."
                  : invitation?.accountExists
                    ? "Accepter l'invitation"
                    : "Accepter l'invitation et créer mon compte"}
              </button>
            </div>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500">
            Déjà un compte ?{" "}
            <a href="/login" className="text-indigo-600 hover:underline">Se connecter</a>
          </p>
        </div>
      </div>
    </div>
  );
  }