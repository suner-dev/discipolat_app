import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import AcceptInvitationPage from "@/pages/AcceptInvitationPage";
import { AuthProvider } from "@/contexts/AuthContext";
import api from "@/lib/api";

// `defaults.headers.common` est utilisé par AuthContext : l'instance mockée
// doit exposer la même surface que l'instance axios réelle.
vi.mock("@/lib/api", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    defaults: { headers: { common: {} as Record<string, string> } },
  },
}));

// Le credential vient du fournisseur : jamais décodé par le client, seulement
// simulé ici pour tester le reste du parcours.
vi.mock("@/features/auth/social/google", () => ({
  requestGoogleCredential: vi.fn(),
}));
vi.mock("@/features/auth/social/microsoft", () => ({
  requestMicrosoftCredential: vi.fn(),
}));
// Facebook passe par une REDIRECTION de page entière : la page est déchargée,
// donc rien ne peut être observé après le clic. On observe donc le point de
// départ du flux et le relais d'invitation.
vi.mock("@/features/auth/social/facebook", () => ({
  startFacebookLogin: vi.fn(),
  stageFacebookInvitation: vi.fn(),
  facebookRedirectUri: () => "https://discipolat.test/auth/social/callback",
}));
vi.mock("@/features/auth/social/providers", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/auth/social/providers")
  >("@/features/auth/social/providers");
  return {
    ...actual,
    fetchSocialProviders: vi.fn(),
  };
});

import { requestGoogleCredential } from "@/features/auth/social/google";
import { requestMicrosoftCredential } from "@/features/auth/social/microsoft";
import { startFacebookLogin, stageFacebookInvitation } from "@/features/auth/social/facebook";
import { fetchSocialProviders } from "@/features/auth/social/providers";

const mockGet = vi.mocked(api.get);
const mockPost = vi.mocked(api.post);

describe("AcceptInvitationPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Défaut : aucun fournisseur servi. Chaque test qui en veut un le déclare.
    vi.mocked(fetchSocialProviders).mockResolvedValue({
      providers: [],
      accountLinkingEnabled: true,
    });
  });

  it("validates an invitation without consuming it", async () => {
    mockGet.mockResolvedValue({
      data: {
        email: "invitee@example.com",
        role: "MEMBER",
        tenantName: "Église Bethel",
        scopeType: "TENANT",
        expiresAt: "2030-01-01T00:00:00Z",
      },
    });

    render(
      <AuthProvider>
        <MemoryRouter initialEntries={["/accept-invitation?token=abc123"]}>
          <AcceptInvitationPage />
        </MemoryRouter>
      </AuthProvider>,
    );

    await waitFor(() => expect(mockGet).toHaveBeenCalledWith(
      "/admin/invitations/validate/abc123",
    ));
    expect(await screen.findByText(/Rejoindre Église Bethel/)).toBeTruthy();
    expect(mockPost).not.toHaveBeenCalled();
  });

  it("rejects a missing token and links to the actual login route", async () => {
    render(
      <AuthProvider>
        <MemoryRouter initialEntries={["/accept-invitation"]}>
          <AcceptInvitationPage />
        </MemoryRouter>
      </AuthProvider>,
    );

    expect(await screen.findByText("Invitation invalide")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Retour à la connexion" })).toBeTruthy();
    expect(mockGet).not.toHaveBeenCalled();
  });

  it("posts the acceptance only after the form is submitted", async () => {
    mockGet.mockResolvedValue({
      data: {
        email: "invitee@example.com",
        role: "MEMBER",
        tenantName: "Église Bethel",
        expiresAt: "2030-01-01T00:00:00Z",
      },
    });
    mockPost.mockResolvedValue({ data: { success: true } });

    render(
      <AuthProvider>
        <MemoryRouter initialEntries={["/accept-invitation?token=abc123"]}>
          <AcceptInvitationPage />
        </MemoryRouter>
      </AuthProvider>,
    );

    await screen.findByText(/Rejoindre Église Bethel/);
    fireEvent.change(screen.getByPlaceholderText("Jean"), { target: { value: "Jean" } });
    fireEvent.change(screen.getByPlaceholderText("Dupont"), { target: { value: "Dupont" } });
    fireEvent.change(screen.getAllByPlaceholderText("••••••••")[0], { target: { value: "password123" } });
    fireEvent.change(screen.getAllByPlaceholderText("••••••••")[1], { target: { value: "password123" } });
    fireEvent.click(screen.getByRole("button", { name: /Accepter l'invitation/ }));

    await waitFor(() => expect(mockPost).toHaveBeenCalledWith(
      "/admin/invitations/accept/abc123",
      expect.objectContaining({ password: "password123" }),
    ));
  });

  it("accepts an existing tenant account without requesting credentials", async () => {
    mockGet.mockResolvedValue({
      data: {
        email: "invitee@example.com",
        role: "MEMBER",
        tenantName: "Église Bethel",
        accountExists: true,
        expiresAt: "2030-01-01T00:00:00Z",
      },
    });
    mockPost.mockResolvedValue({ data: { success: true } });

    render(
      <AuthProvider>
        <MemoryRouter initialEntries={["/accept-invitation?token=abc123"]}>
          <AcceptInvitationPage />
        </MemoryRouter>
      </AuthProvider>,
    );

    await screen.findByText(/Un compte existe déjà/);
    expect(screen.queryByPlaceholderText("Jean")).toBeNull();
    expect(screen.queryByPlaceholderText("••••••••")).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Accepter l'invitation" }));

    await waitFor(() => expect(mockPost).toHaveBeenCalledWith(
      "/admin/invitations/accept/abc123",
      {},
    ));
  });

  // =========================================================================
  // Acceptation par identité externe (Google / Microsoft) — sans mot de passe
  // =========================================================================

  describe("acceptation par identité externe", () => {
    const invitationResponse = {
      data: {
        email: "invitee@example.com",
        role: "MEMBRE",
        tenantName: "Église Bethel",
        scopeType: "TENANT",
        accountExists: false,
        expiresAt: "2030-01-01T00:00:00Z",
      },
    };

    beforeEach(() => {
      mockGet.mockResolvedValue(invitationResponse);
      vi.mocked(fetchSocialProviders).mockResolvedValue({
        providers: [{ provider: "google", label: "Google" }],
        accountLinkingEnabled: true,
      });
      vi.stubEnv("VITE_GOOGLE_CLIENT_ID", "web-client.apps.googleusercontent.com");
    });

    afterEach(() => {
      vi.unstubAllEnvs();
    });

    it("n'affiche aucun bouton si le serveur ne sert aucun fournisseur", async () => {
      vi.mocked(fetchSocialProviders).mockResolvedValue({
        providers: [],
        accountLinkingEnabled: true,
      });

      render(
        <AuthProvider>
          <MemoryRouter initialEntries={["/accept-invitation?token=abc123"]}>
            <AcceptInvitationPage />
          </MemoryRouter>
        </AuthProvider>,
      );

      expect(await screen.findByText(/Rejoindre Église Bethel/)).toBeTruthy();
      expect(
        screen.queryByRole("button", { name: /Accepter l'invitation avec Google/ }),
      ).toBeNull();
    });

    it("accepte l'invitation avec Google et ouvre la session, sans mot de passe", async () => {
      vi.mocked(requestGoogleCredential).mockResolvedValue("google-credential");
      mockPost.mockResolvedValue({
        data: {
          success: true,
          provider: "google",
          identityCreated: true,
          userId: "user-1",
          email: "invitee@example.com",
          tenantId: "tenant-1",
          alreadyMember: false,
          crossTenantIdentity: false,
          welcomeEmailSent: true,
          accessToken: "access",
          refreshToken: "refresh",
          role: "MEMBRE",
          roles: ["MEMBRE"],
          activeRole: "MEMBRE",
          firstName: "Paul",
          lastName: "Koffi",
          platformRoles: [],
          platformSuperAdmin: false,
        },
      });

      render(
        <AuthProvider>
          <MemoryRouter initialEntries={["/accept-invitation?token=abc123"]}>
            <AcceptInvitationPage />
          </MemoryRouter>
        </AuthProvider>,
      );

      const button = await screen.findByRole("button", {
        name: /Accepter l'invitation avec Google/,
      });
      fireEvent.click(button);

      await waitFor(() =>
        expect(mockPost).toHaveBeenCalledWith(
          "/admin/invitations/accept-identity/abc123",
          expect.objectContaining({ provider: "google", credential: "google-credential" }),
        ),
      );
      // Le mot de passe n'est jamais demandé ni transmis dans ce chemin.
      expect(mockPost).toHaveBeenCalledWith(
        "/admin/invitations/accept-identity/abc123",
        expect.not.objectContaining({ password: expect.anything() }),
      );
    });

    it("affiche un message clair si l'identité ne correspond pas à l'adresse invitée", async () => {
      vi.mocked(requestGoogleCredential).mockResolvedValue("credential");
      mockPost.mockRejectedValue({
        response: {
          status: 403,
          data: {
            title: "SOCIAL_EMAIL_MISMATCH",
            detail: "L'identite verifiee ne correspond pas a l'adresse invitée",
          },
        },
      });

      render(
        <AuthProvider>
          <MemoryRouter initialEntries={["/accept-invitation?token=abc123"]}>
            <AcceptInvitationPage />
          </MemoryRouter>
        </AuthProvider>,
      );

      fireEvent.click(
        await screen.findByRole("button", {
          name: /Accepter l'invitation avec Google/,
        }),
      );

      expect(
        await screen.findByText(/ne correspond pas à l'adresse invitée/),
      ).toBeTruthy();
    });

    it("reste silencieux si l'utilisateur ferme la fenêtre du fournisseur", async () => {
      vi.mocked(requestGoogleCredential).mockRejectedValue(
        Object.assign(new Error("annulée"), {
          name: "SocialAuthError",
          code: "SOCIAL_LOGIN_CANCELLED",
        }),
      );

      render(
        <AuthProvider>
          <MemoryRouter initialEntries={["/accept-invitation?token=abc123"]}>
            <AcceptInvitationPage />
          </MemoryRouter>
        </AuthProvider>,
      );

      fireEvent.click(
        await screen.findByRole("button", {
          name: /Accepter l'invitation avec Google/,
        }),
      );

      await waitFor(() => expect(requestGoogleCredential).toHaveBeenCalled());
      expect(mockPost).not.toHaveBeenCalled();
    });

    // =======================================================================
    // Facebook : le fournisseur qui a révélé le bug le plus grave.
    //
    // Facebook impose une REDIRECTION de page entière (et non une popup), donc
    // `handleAcceptWithIdentity` ne peut pas suivre le même chemin que
    // Google/Microsoft. Le test verrouille trois choses :
    //   1. le bouton Facebook n'ouvre PAS le dialogue Microsoft (le clientId
    //      était résolu vers `clientIds.microsoft` pour tout fournisseur autre
    //      que Google : un clic sur « Facebook » lançait silencieusement
    //      Microsoft) ;
    //   2. l'invitation est confiée au relais, sinon le jeton — qui porte le
    //      rôle et l'église — est perdu lors du changement de page ;
    //   3. le relais n'est écrit qu'avec un App ID réellement configuré.
    // =======================================================================
    describe("acceptation par Facebook (redirection)", () => {
      const invitationResponse = {
        data: {
          email: "invitee@example.com",
          role: "MEMBRE",
          tenantName: "Église Bethel",
          scopeType: "TENANT",
          accountExists: false,
          expiresAt: "2030-01-01T00:00:00Z",
        },
      };

      const renderInvitation = () =>
        render(
          <AuthProvider>
            <MemoryRouter initialEntries={["/accept-invitation?token=abc123"]}>
              <AcceptInvitationPage />
            </MemoryRouter>
          </AuthProvider>,
        );

      beforeEach(() => {
        mockGet.mockResolvedValue(invitationResponse);
        sessionStorage.clear();
        vi.mocked(fetchSocialProviders).mockResolvedValue({
          providers: [{ provider: "facebook", label: "Facebook" }],
          accountLinkingEnabled: true,
        });
        vi.stubEnv("VITE_FACEBOOK_APP_ID", "1234567890123456");
        vi.stubEnv("VITE_MICROSOFT_CLIENT_ID", "microsoft-client-id");
      });

      afterEach(() => {
        vi.unstubAllEnvs();
        sessionStorage.clear();
      });

      it("n'ouvre JAMAIS le dialogue Microsoft quand on clique sur Facebook", async () => {
        renderInvitation();

        fireEvent.click(
          await screen.findByRole("button", {
            name: /Accepter l'invitation avec Facebook/,
          }),
        );

        // Le point du correctif : ni popup, ni requête d'acceptation.
        await waitFor(() => expect(startFacebookLogin).toHaveBeenCalled());
        expect(requestMicrosoftCredential).not.toHaveBeenCalled();
        expect(requestGoogleCredential).not.toHaveBeenCalled();
        expect(mockPost).not.toHaveBeenCalled();
      });

      it("utilise l'App ID Facebook, et non un autre identifiant", async () => {
        renderInvitation();

        fireEvent.click(
          await screen.findByRole("button", {
            name: /Accepter l'invitation avec Facebook/,
          }),
        );

        await waitFor(() =>
          expect(startFacebookLogin).toHaveBeenCalledWith(
            expect.objectContaining({ appId: "1234567890123456" }),
          ),
        );
      });

      it("confie le jeton d'invitation au relais avant de rediriger", async () => {
        renderInvitation();

        fireEvent.click(
          await screen.findByRole("button", {
            name: /Accepter l'invitation avec Facebook/,
          }),
        );

        // Sans ce relais, le retour sur /auth/social/callback perdrait le
        // jeton d'invitation et l'utilisateur retomberait sur une simple
        // connexion au lieu d'accepter son invitation.
        await waitFor(() =>
          expect(stageFacebookInvitation).toHaveBeenCalledWith(
            expect.objectContaining({ token: "abc123" }),
          ),
        );
        // Le relais doit être posé AVANT la redirection : l'ordre n'est pas
        // décoratif, c'est lui qui rend le flux récupérable.
        const stageOrder = vi
          .mocked(stageFacebookInvitation)
          .mock.invocationCallOrder[0];
        expect(stageOrder).toBeLessThan(vi.mocked(startFacebookLogin).mock.invocationCallOrder[0]);
      });

      it("refuse de démarrer si l'App ID Facebook est absent de la build", async () => {
        vi.stubEnv("VITE_FACEBOOK_APP_ID", "");
        renderInvitation();

        fireEvent.click(
          await screen.findByRole("button", {
            name: /Accepter l'invitation avec Facebook/,
          }),
        );

        // Fail-closed : aucun dialogue ouvert, aucun relais écrit, un message
        // explicite plutôt qu'un échec opaque plus tard.
        await waitFor(() =>
          expect(screen.getByText(/n'est pas configuré sur ce serveur/)).toBeTruthy(),
        );
        expect(startFacebookLogin).not.toHaveBeenCalled();
        expect(stageFacebookInvitation).not.toHaveBeenCalled();
      });
    });
  });
});
