import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import api from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";
import { requestGoogleCredential } from "@/features/auth/social/google";
import { requestMicrosoftCredential } from "@/features/auth/social/microsoft";
import {
  facebookRedirectUri,
  stageFacebookInvitation,
  startFacebookLogin,
} from "@/features/auth/social/facebook";
import {
  buildTimeClientIds,
  fetchSocialProviders,
  toSocialAuthError,
} from "@/features/auth/social/providers";
import type { SocialProviderId, SocialProvidersState } from "@/features/auth/social/types";

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
  const { loginWithSocialToken } = useAuth();
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
  // Connexion par identité externe : évite d'imposer un mot de passe à quelqu'un
  // qui n'en veut pas. Le rôle et l'église viennent TOUJOURS de l'invitation.
  const [socialState, setSocialState] = useState<SocialProvidersState | null>(null);
  const [socialPending, setSocialPending] = useState<SocialProviderId | null>(null);

  useEffect(() => {
    if (!token) {
      setError("Token d'invitation manquant");
      setLoading(false);
      return;
    }
    loadInvitation();
  }, [token]);

  useEffect(() => {
    let active = true;
    fetchSocialProviders()
      .then((providers) => {
        if (active) setSocialState(providers);
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, []);

  /**
   * Accepte l'invitation avec une identité externe vérifiée
   * (Google / Microsoft / Facebook).
   *
   * <p>Le backend refuse si l'email vérifié par le fournisseur n'est pas celui de
   * l'invitation : l'interface n'a donc rien à vérifier elle-même, et affiche le
   * message du serveur tel quel.
   */
  const handleAcceptWithIdentity = async (provider: SocialProviderId) => {
    if (!token) return;
    const clientIds = buildTimeClientIds();
    const clientId =
      provider === "google"
        ? clientIds.google
        : provider === "microsoft"
          ? clientIds.microsoft
          : clientIds.facebook;
    if (!clientId) {
      setError("Ce mode de connexion n'est pas configuré sur ce serveur.");
      return;
    }

    if (provider === "facebook") {
      // Facebook impose une REDIRECTION de page entière : la popup Google et le
      // dialogue Microsoft en PKCE ne s'appliquent pas. L'invitation est donc
      // confiée au relais sessionStorage, et la page d'accueil du dialogue la
      // reprend au retour sur /auth/social/callback. Sans cela, le jeton
      // d'invitation — qui porte le rôle et l'église — serait perdu.
      stageFacebookInvitation({
        token,
        firstName: formData.firstName || undefined,
        lastName: formData.lastName || undefined,
      });
      try {
        startFacebookLogin({
          appId: clientId,
          apiVersion: import.meta.env.VITE_FACEBOOK_API_VERSION || "v21.0",
          redirectUri: facebookRedirectUri(),
        });
      } catch (err) {
        setError(toSocialAuthError(err).message);
      }
      return;
    }

    setSocialPending(provider);
    setError(null);
    try {
      const credential =
        provider === "google"
          ? await requestGoogleCredential(clientId)
          : await requestMicrosoftCredential(
              clientId,
              import.meta.env.VITE_MICROSOFT_TENANT_ID || "common"
            );

      const res = await api.post(
        `/admin/invitations/accept-identity/${encodeURIComponent(token)}`,
        {
          provider,
          credential,
          // Saisie prioritaire ; sinon le backend déduit du nom du fournisseur.
          firstName: formData.firstName || undefined,
          lastName: formData.lastName || undefined,
        }
      );

      const data = res.data;
      // Session ouverte immédiatement : l'invité n'a pas à ressaisir ses identifiants.
      loginWithSocialToken(
        data.accessToken,
        {
          id: data.userId,
          email: data.email,
          firstName: data.firstName ?? undefined,
          lastName: data.lastName ?? undefined,
          role: data.role,
        },
        data.refreshToken
      );
      setSuccess(true);
      setTimeout(() => navigate("/dashboard", { replace: true }), 1500);
    } catch (err) {
      const failure = toSocialAuthError(err);
      if (failure.code === "SOCIAL_LOGIN_CANCELLED") return;
      if (failure.code === "SOCIAL_EMAIL_MISMATCH") {
        setError(
          "L'adresse de votre compte externe ne correspond pas à l'adresse invitée."
        );
        return;
      }
      setError(failure.message);
    } finally {
      setSocialPending(null);
    }
  };

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
            <p className="mt-2 text-sm text-gray-500">
              {socialPending !== null || localStorage.getItem('accessToken')
                ? 'Redirection vers votre espace…'
                : 'Redirection vers la connexion dans 3 secondes...'}
            </p>
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

          {/* Connexion par identité externe — pour un nouveau membre qui ne veut
              pas créer de mot de passe. Le rôle affiché ci-dessus (MEMBRE,
              PASTEUR…) vient de l'invitation : le fournisseur ne donne aucun
              droit, il prouve seulement l'identité. */}
          {!invitation?.accountExists && socialState && socialState.providers.length > 0 && (
            <div className="mt-6">
              <div className="relative flex items-center">
                <div className="flex-1 border-t border-gray-200" />
                <span className="px-3 text-xs text-gray-400">ou</span>
                <div className="flex-1 border-t border-gray-200" />
              </div>
              <div className="mt-4 space-y-3">
                {socialState.providers.map(({ provider, label }) => (
                  <button
                    key={provider}
                    type="button"
                    onClick={() => handleAcceptWithIdentity(provider)}
                    disabled={accepting || socialPending !== null}
                    aria-busy={socialPending === provider}
                    className="w-full py-2.5 px-4 rounded-xl border border-gray-200 bg-white
                               text-gray-700 font-medium text-sm hover:bg-gray-50
                               transition-colors flex items-center justify-center gap-3
                               disabled:opacity-60 disabled:cursor-not-allowed
                               focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500"
                  >
                    {socialPending === provider
                      ? "Connexion…"
                      : `Accepter l'invitation avec ${label}`}
                  </button>
                ))}
              </div>
              <p className="mt-3 text-[11px] text-gray-500 text-center">
                L'adresse vérifiée par {socialState.providers.map((entry) => entry.label).join(' ou ')} doit
                correspondre à <strong>{invitation?.email}</strong>.
              </p>
            </div>
          )}

          <p className="mt-6 text-center text-sm text-gray-500">
            Déjà un compte ?{" "}
            <a href="/login" className="text-indigo-600 hover:underline">Se connecter</a>
          </p>
        </div>
      </div>
    </div>
  );
  }