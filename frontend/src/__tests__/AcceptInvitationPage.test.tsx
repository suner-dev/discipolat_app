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
  });
});
