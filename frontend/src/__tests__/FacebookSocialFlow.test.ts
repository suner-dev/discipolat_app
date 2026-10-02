import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  clearFacebookHandoff,
  facebookRedirectUri,
  readFacebookCredential,
  stageFacebookInvitation,
  startFacebookLogin,
  takeFacebookInvitation,
} from '@/features/auth/social/facebook';

/**
 * Sécurité du flux Facebook (OIDC par redirection).
 *
 * Le point critique est le `state` : sans lui, un attaquant pourrait injecter
 * SON `id_token` dans la session d'un utilisateur. Ces tests verrouillent ce
 * contrôle, la fenêtre de fraîcheur, et le nettoyage de l'URL.
 */
describe('flux Facebook (OIDC par redirection)', () => {
  const APP_ID = '1234567890123456';
  const STORAGE_KEY = 'discipolat:social-handoff';

  const setFragment = (fragment: string) => {
    window.location.hash = fragment;
  };

  beforeEach(() => {
    sessionStorage.clear();
    window.location.hash = '';
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  describe('startFacebookLogin', () => {
    it('redirige vers le dialogue Meta avec response_type=id_token', () => {
      const assign = vi.fn();
      vi.spyOn(window, 'location', 'get').mockReturnValue({
        origin: 'https://discipolat.onrender.com',
        assign,
      } as unknown as Location);

      startFacebookLogin({
        appId: APP_ID,
        redirectUri: 'https://discipolat.onrender.com/auth/social/callback',
      });

      expect(assign).toHaveBeenCalledTimes(1);
      const url = assign.mock.calls[0][0] as string;
      // `id_token` et non `token` : c'est la seule réponse vérifiable par JWKS.
      expect(url).toContain('response_type=id_token');
      expect(url).toContain(`client_id=${APP_ID}`);
      expect(url).toContain('scope=email%2Cpublic_profile');
      expect(url).toContain('/v21.0/dialog/oauth');
    });

    it('dépouille un state aléatoire de 32 octets, différent à chaque appel', () => {
      const assign = vi.fn();
      vi.spyOn(window, 'location', 'get').mockReturnValue({
        origin: 'https://discipolat.onrender.com',
        assign,
      } as unknown as Location);

      startFacebookLogin({ appId: APP_ID, redirectUri: 'https://x/cb' });
      const first = JSON.parse(sessionStorage.getItem(STORAGE_KEY) as string).state as string;

      startFacebookLogin({ appId: APP_ID, redirectUri: 'https://x/cb' });
      const second = JSON.parse(sessionStorage.getItem(STORAGE_KEY) as string).state as string;

      expect(first).toHaveLength(64);
      expect(second).toHaveLength(64);
      expect(first).not.toBe(second);
      expect(assign).toHaveBeenCalledTimes(2);
    });

    it('refuse de démarrer sans App ID (fail-closed)', () => {
      expect(() => startFacebookLogin({ appId: '', redirectUri: 'https://x/cb' })).toThrow(
        /n’est pas configurée/i
      );
    });
  });

  describe('readFacebookCredential', () => {
    const seedHandoff = (state: string, issuedAt = Date.now()) => {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ state, provider: 'facebook', issuedAt }));
    };

    it('rend le credential quand state et fraîcheur sont conformes', () => {
      const state = 'a'.repeat(64);
      seedHandoff(state);
      setFragment(`#id_token=jwt.signed.by.facebook&state=${state}`);

      expect(readFacebookCredential()).toBe('jwt.signed.by.facebook');
    });

    it('REFUSE un state qui ne correspond pas (protection CSRF)', () => {
      seedHandoff('a'.repeat(64));
      setFragment('#id_token=jeton.attaquant&state=bbbbbbbb');

      // C'est LE test de sécurité de ce fichier : sans ce contrôle, un
      // attaquant pourrait faire connecter une victime avec SON compte.
      expect(() => readFacebookCredential()).toThrow(/non sécurisée/i);
    });

    it('REFUSE un state absent', () => {
      seedHandoff('a'.repeat(64));
      setFragment('#id_token=jeton');

      expect(() => readFacebookCredential()).toThrow(/non sécurisée/i);
    });

    it('refuse un relais plus vieux de 5 minutes (rejeu)', () => {
      const state = 'a'.repeat(64);
      seedHandoff(state, Date.now() - 5 * 60 * 1000 - 1000);
      setFragment(`#id_token=jwt&state=${state}`);

      expect(() => readFacebookCredential()).toThrow(/expirée/i);
    });

    it('refuse un relais absent', () => {
      setFragment('#id_token=jwt&state=abc');
      expect(() => readFacebookCredential()).toThrow(/expirée|invalid/i);
    });

    it('traduit une annulation en code silencieux', () => {
      const state = 'a'.repeat(64);
      seedHandoff(state);
      setFragment(`#error=access_denied&state=${state}`);

      try {
        readFacebookCredential();
        throw new Error('devait lever');
      } catch (error) {
        expect((error as { code?: string }).code).toBe('SOCIAL_LOGIN_CANCELLED');
      }
    });

    it('traduit un refus Facebook en erreur non silencieuse', () => {
      const state = 'a'.repeat(64);
      seedHandoff(state);
      setFragment(`#error=server_error&state=${state}`);

      try {
        readFacebookCredential();
        throw new Error('devait lever');
      } catch (error) {
        expect((error as { code?: string }).code).toBe('SOCIAL_CREDENTIAL_REJECTED');
      }
    });

    it('consomme le relais : un second appel échoue', () => {
      const state = 'a'.repeat(64);
      seedHandoff(state);
      setFragment(`#id_token=jwt&state=${state}`);

      readFacebookCredential();

      expect(sessionStorage.getItem(STORAGE_KEY)).toBeNull();
      expect(() => readFacebookCredential()).toThrow();
    });

    /**
     * Le jeton ne doit jamais rester dans l'historique, y compris quand il est
     * REJETÉ. Nettoyer l'URL uniquement en cas de succès laissait le `id_token`
     * dans la barre d'adresse précisément dans le cas qui ne doit jamais
     * arriver — et le laisser accessible en cas de state non conforme revenait
     * à afficher le jeton d'un attaquant à l'écran.
     */
    it.each([
      ['state non conforme', '#id_token=jeton&state=bbbbbbbb', 'a'.repeat(64)],
      ['id_token absent', '#state=abc', 'a'.repeat(64)],
      ['retour sans state', '#id_token=jeton', 'a'.repeat(64)],
    ])('nettoie le fragment même en cas de refus : %s', (_label, fragment, handoffState) => {
      seedHandoff(handoffState);
      setFragment(fragment);

      const replaceState = vi.spyOn(window.history, 'replaceState');
      expect(() => readFacebookCredential()).toThrow();
      expect(replaceState).toHaveBeenCalled();
    });
  });

  describe('clearFacebookHandoff', () => {
    it('supprime le relais', () => {
      sessionStorage.setItem(STORAGE_KEY, '{}');
      clearFacebookHandoff();
      expect(sessionStorage.getItem(STORAGE_KEY)).toBeNull();
    });
  });

  describe('facebookRedirectUri', () => {
    it('vise la route de retour sur la même origine', () => {
      expect(facebookRedirectUri()).toBe(`${window.location.origin}/auth/social/callback`);
    });
  });

  /**
   * Relais d'invitation.
   *
   * <p>Facebook impose une redirection de page entière : la page d'acceptation
   * d'invitation est déchargée. Sans relais, le jeton d'invitation — qui porte le
   * rôle et l'église — est perdu, et l'utilisateur retombe sur une connexion
   * simple au lieu d'accepter son invitation. Ces tests verrouillent les deux
   * propriétés qui comptent : il restitue ce qu'on lui a confié, et il ne se
   * rejoue pas.
   */
  describe('relais d’invitation', () => {
    const INVITATION_KEY = 'discipolat:social-invitation';

    it('restitue le jeton et les noms saisis', () => {
      stageFacebookInvitation({ token: 'abc123', firstName: 'Paul', lastName: 'Koffi' });

      expect(takeFacebookInvitation()).toEqual({
        token: 'abc123',
        firstName: 'Paul',
        lastName: 'Koffi',
      });
    });

    it('NE SE REJOUE PAS : la lecture consomme le relais', () => {
      stageFacebookInvitation({ token: 'abc123' });

      takeFacebookInvitation();

      // Un jeton d'invitation est un secret d'acceptation : le laisser relisible
      // le rendrait rejouable depuis l'historique du navigateur.
      expect(sessionStorage.getItem(INVITATION_KEY)).toBeNull();
      expect(takeFacebookInvitation()).toBeNull();
    });

    it('renvoie null si aucun relais n’est en attente', () => {
      expect(takeFacebookInvitation()).toBeNull();
    });

    it('ignore un relais corrompu ou sans jeton', () => {
      sessionStorage.setItem(INVITATION_KEY, 'pas-du-json');
      expect(takeFacebookInvitation()).toBeNull();

      stageFacebookInvitation({ token: '' } as { token: string });
      expect(takeFacebookInvitation()).toBeNull();
    });

    it('normalise les champs vides en undefined', () => {
      stageFacebookInvitation({ token: 'abc123', firstName: '', lastName: '' });

      expect(takeFacebookInvitation()).toEqual({
        token: 'abc123',
        firstName: undefined,
        lastName: undefined,
      });
    });
  });
});
