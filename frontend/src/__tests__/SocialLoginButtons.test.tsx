import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SocialLoginButtons } from '@/features/auth/social/SocialLoginButtons';
import { I18nProvider } from '@/i18n';
import api from '@/lib/api';
import { resetSocialProvidersCache } from '@/features/auth/social/providers';

// Le script Google et MSAL ne sont jamais chargés dans un test : on simule
// directement la promesse de credential, qui est la seule chose que le
// navigateur produit et que le serveur vérifie.
vi.mock('@/features/auth/social/google', () => ({
  requestGoogleCredential: vi.fn(),
}));
vi.mock('@/features/auth/social/microsoft', () => ({
  requestMicrosoftCredential: vi.fn(),
}));

import { requestGoogleCredential } from '@/features/auth/social/google';
import { requestMicrosoftCredential } from '@/features/auth/social/microsoft';

function mockProviders(providers: string[]) {
  vi.spyOn(api, 'get').mockResolvedValue({
    data: {
      providers: providers.map((provider) => ({ provider, label: provider })),
      accountLinkingEnabled: true,
    },
  } as never);
}

function renderButtons(props: Partial<React.ComponentProps<typeof SocialLoginButtons>> = {}) {
  return render(
    <I18nProvider>
      <SocialLoginButtons
        onAuthenticated={props.onAuthenticated ?? vi.fn()}
        onErrorChange={props.onErrorChange}
        error={props.error ?? null}
        disabled={props.disabled}
      />
    </I18nProvider>
  );
}

describe('SocialLoginButtons', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetSocialProvidersCache();
    vi.stubEnv('VITE_GOOGLE_CLIENT_ID', 'web-client.apps.googleusercontent.com');
    vi.stubEnv('VITE_MICROSOFT_CLIENT_ID', 'ms-client-id');
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.restoreAllMocks();
  });

  it('n’affiche aucun bouton quand le serveur n’a aucun fournisseur configuré', async () => {
    mockProviders([]);

    const { container } = renderButtons();

    await waitFor(() => {
      expect(container.querySelector('[data-testid="social-login-buttons"]')).toBeNull();
    });
  });

  it('n’affiche que les fournisseurs réellement servis par l’API', async () => {
    // Point clé : le build peut embarquer un client-id que le serveur ne sert
    // pas. L'API fait foi, sinon l'utilisateur clique et reçoit un 503.
    mockProviders(['google']);

    renderButtons();

    expect(await screen.findByTestId('social-button-google')).toBeInTheDocument();
    expect(screen.queryByTestId('social-button-microsoft')).not.toBeInTheDocument();
  });

  it('affiche les deux boutons quand Google et Microsoft sont actifs', async () => {
    mockProviders(['google', 'microsoft']);

    renderButtons();

    expect(await screen.findByTestId('social-button-google')).toBeInTheDocument();
    expect(screen.findByTestId('social-button-microsoft')).toBeTruthy();
  });

  it('connecte avec Google et remonte les jetons au parent', async () => {
    mockProviders(['google']);
    vi.mocked(requestGoogleCredential).mockResolvedValue('google-credential');
    vi.spyOn(api, 'post').mockResolvedValue({
      data: {
        accessToken: 'access',
        refreshToken: 'refresh',
        userId: 'user-1',
        email: 'paul@exemple.com',
        firstName: 'Paul',
        lastName: 'Koffi',
        role: 'MEMBRE',
        activeRole: 'MEMBRE',
      },
    } as never);
    const onAuthenticated = vi.fn();

    renderButtons({ onAuthenticated });

    await userEvent.click(await screen.findByTestId('social-button-google'));

    await waitFor(() => {
      expect(onAuthenticated).toHaveBeenCalledWith(
        expect.objectContaining({
          accessToken: 'access',
          refreshToken: 'refresh',
          user: expect.objectContaining({ email: 'paul@exemple.com', role: 'MEMBRE' }),
        })
      );
    });
    expect(requestGoogleCredential).toHaveBeenCalledWith('web-client.apps.googleusercontent.com');
    // Le credential n'est jamais interpreté côté client : il est transmis tel quel.
    expect(api.post).toHaveBeenCalledWith('/auth/social/google', {
      credential: 'google-credential',
    });
  });

  it('demande une action concrète quand aucun compte ne correspond à l’adresse', async () => {
    mockProviders(['google']);
    vi.mocked(requestGoogleCredential).mockResolvedValue('credential');
    vi.spyOn(api, 'post').mockRejectedValue({
      response: {
        status: 403,
        data: {
          title: 'SOCIAL_ACCOUNT_NOT_LINKED',
          detail: "Aucun compte Discipolat pour cette adresse.",
        },
      },
    } as never);
    const onErrorChange = vi.fn();

    renderButtons({ onErrorChange });

    await userEvent.click(await screen.findByTestId('social-button-google'));

    await waitFor(() => {
      expect(onErrorChange).toHaveBeenCalledWith(
        expect.stringContaining("lien d'invitation")
      );
    });
  });

  it('retire le bouton quand le serveur déclare le fournisseur non configuré', async () => {
    mockProviders(['google', 'microsoft']);
    vi.mocked(requestGoogleCredential).mockResolvedValue('credential');
    vi.spyOn(api, 'post').mockRejectedValue({
      response: {
        status: 503,
        data: { title: 'SOCIAL_PROVIDER_NOT_CONFIGURED', detail: 'non configuré' },
      },
    } as never);

    renderButtons();

    await userEvent.click(await screen.findByTestId('social-button-google'));

    // Le bouton disparaît au lieu de laisser l'utilisateur réessayer pour rien.
    await waitFor(() => {
      expect(screen.queryByTestId('social-button-google')).not.toBeInTheDocument();
    });
    expect(screen.getByTestId('social-button-microsoft')).toBeInTheDocument();
  });

  it('reste silencieux si l’utilisateur ferme la fenêtre du fournisseur', async () => {
    mockProviders(['google']);
    vi.mocked(requestGoogleCredential).mockRejectedValue(
      Object.assign(new Error('annulée'), { name: 'SocialAuthError', code: 'SOCIAL_LOGIN_CANCELLED' })
    );
    const onErrorChange = vi.fn();

    renderButtons({ onErrorChange });

    await userEvent.click(await screen.findByTestId('social-button-google'));

    await waitFor(() => {
      expect(onErrorChange).toHaveBeenCalledWith(null);
    });
    expect(onErrorChange).not.toHaveBeenCalledWith(
      expect.stringContaining('erreur')
    );
  });

  it('affiche le message d’erreur avec role="alert" (accessibilité)', async () => {
    mockProviders(['google']);

    renderButtons({ error: 'Connexion impossible.' });

    const alert = await screen.findByTestId('social-login-error');
    expect(alert).toHaveAttribute('role', 'alert');
    expect(alert).toHaveTextContent('Connexion impossible.');
  });

  it('désactive les boutons pendant une connexion en cours', async () => {
    mockProviders(['google']);
    let resolveCredential: (value: string) => void = () => {};
    vi.mocked(requestGoogleCredential).mockReturnValue(
      new Promise<string>((resolve) => {
        resolveCredential = resolve;
      })
    );
    vi.spyOn(api, 'post').mockResolvedValue({ data: {} } as never);

    renderButtons();

    const button = await screen.findByTestId('social-button-google');
    await userEvent.click(button);

    await waitFor(() => {
      expect(button).toBeDisabled();
    });

    resolveCredential('credential');
  });

  it('affiche Microsoft sans jamais charger Google', async () => {
    mockProviders(['microsoft']);
    vi.mocked(requestMicrosoftCredential).mockResolvedValue('ms-credential');
    vi.spyOn(api, 'post').mockResolvedValue({
      data: {
        accessToken: 'access',
        refreshToken: 'refresh',
        userId: 'user-1',
        email: 'paul@exemple.com',
        role: 'MEMBRE',
      },
    } as never);

    renderButtons();

    await userEvent.click(await screen.findByTestId('social-button-microsoft'));

    await waitFor(() => {
      expect(requestMicrosoftCredential).toHaveBeenCalledWith('ms-client-id', 'common');
    });
    expect(requestGoogleCredential).not.toHaveBeenCalled();
  });
});
