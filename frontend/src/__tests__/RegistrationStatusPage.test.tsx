// B3 — Tests de la page de suivi de demande d'inscription.
// 5 cas exigés par le plan : NONE, PENDING, APPROVED + bouton login,
// REJECTED + motif, 429.

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import RegistrationStatusPage from '@/pages/RegistrationStatusPage';

vi.mock('@/lib/api', () => ({ default: { post: vi.fn() } }));
import api from '@/lib/api';
const mocked = api as unknown as { post: ReturnType<typeof vi.fn> };

function renderPage(search = '') {
  return render(
    <MemoryRouter initialEntries={[`/registration-status${search}`]}>
      <RegistrationStatusPage />
    </MemoryRouter>,
  );
}

async function submit(email = 'pasteur@eglise.com') {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText(/Adresse email/), email);
  await user.click(screen.getByRole('button', { name: /Vérifier le statut/ }));
}

beforeEach(() => vi.clearAllMocks());
afterEach(() => vi.restoreAllMocks());

describe('B3 — les 4 statuts du contrat §3.3', () => {
  it('NONE : Aucune demande trouvée + lien d\u2019inscription', async () => {
    mocked.post.mockResolvedValue({
      data: { status: 'NONE', decidedAt: null, reason: null, canLogin: false },
    });
    renderPage();
    await submit();
    await waitFor(() => expect(screen.getByText('Aucune demande trouvée')).toBeTruthy());
    expect(screen.getByRole('link', { name: /Demander une inscription/ })).toBeTruthy();
  });

  it('PENDING_APPROVAL : message d\u2019examen, AUCUN bouton de connexion', async () => {
    mocked.post.mockResolvedValue({
      data: { status: 'PENDING_APPROVAL', decidedAt: null, reason: null, canLogin: false },
    });
    renderPage();
    await submit();
    await waitFor(() => expect(screen.getByText('Demande en cours d\u2019examen')).toBeTruthy());
    expect(screen.queryByRole('link', { name: 'Se connecter' })).toBeNull();
  });

  it('APPROVED : bouton « Se connecter » quand canLogin=true', async () => {
    mocked.post.mockResolvedValue({
      data: { status: 'APPROVED', decidedAt: '2026-09-28T10:00:00Z', reason: null, canLogin: true },
    });
    renderPage();
    await submit();
    await waitFor(() => expect(screen.getByText('Demande approuvée')).toBeTruthy());
    const link = screen.getByRole('link', { name: 'Se connecter' });
    expect(link.getAttribute('href')).toBe('/login');
  });

  it('REJECTED : le motif EST affiché', async () => {
    mocked.post.mockResolvedValue({
      data: { status: 'REJECTED', decidedAt: '2026-09-28T10:00:00Z',
        reason: 'Dossier incomplet', canLogin: false },
    });
    renderPage();
    await submit();
    await waitFor(() => expect(screen.getByText('Demande refusée')).toBeTruthy());
    expect(screen.getByText(/Dossier incomplet/)).toBeTruthy();
  });

  it('REJECTED : le motif n\u2019est PAS divulgué pour les autres statuts', async () => {
    mocked.post.mockResolvedValue({
      // Un backend malveillant qui renverrait reason hors REJECTED :
      // le client ne doit pas l'afficher (contrat §3.3).
      data: { status: 'PENDING_APPROVAL', decidedAt: null,
        reason: 'FUITE INTERNE', canLogin: false },
    });
    renderPage();
    await submit();
    await waitFor(() => expect(screen.getByText('Demande en cours d\u2019examen')).toBeTruthy());
    expect(screen.queryByText(/FUITE INTERNE/)).toBeNull();
  });
});

describe('B3 — cas limites', () => {
  it('429 : message d\u2019attente, pas une erreur fatale', async () => {
    mocked.post.mockRejectedValue({ response: { status: 429 } });
    renderPage();
    await submit();
    await waitFor(() => expect(screen.getByText(/Trop de vérifications/)).toBeTruthy());
  });

  it('email pré-rempli depuis ?email= (lien depuis RegisterPage)', async () => {
    mocked.post.mockResolvedValue({
      data: { status: 'NONE', decidedAt: null, reason: null, canLogin: false },
    });
    renderPage('?email=prefilled%40eglise.com');
    await waitFor(() => {
      const input = screen.getByLabelText(/Adresse email/) as HTMLInputElement;
      expect(input.value).toBe('prefilled@eglise.com');
    });
  });

  it('statut inconnu renvoyé par le backend -> traité comme NONE (pas de crash)', async () => {
    mocked.post.mockResolvedValue({
      data: { status: 'BLABLA_INCONNU', decidedAt: null, reason: null, canLogin: true },
    });
    renderPage();
    await submit();
    await waitFor(() => expect(screen.getByText('Aucune demande trouvée')).toBeTruthy());
  });

  it('envoie l\u2019email au bon endpoint avec POST', async () => {
    mocked.post.mockResolvedValue({
      data: { status: 'NONE', decidedAt: null, reason: null, canLogin: false },
    });
    renderPage();
    await submit('contact@exemple.org');
    await waitFor(() => expect(mocked.post).toHaveBeenCalled());
    expect(mocked.post).toHaveBeenCalledWith('/auth/registration-status', { email: 'contact@exemple.org' });
  });

  it('refuse un email invalide sans appeler l\u2019API', async () => {
    renderPage();
    await submit('pas-un-email');
    expect(mocked.post).not.toHaveBeenCalled();
  });
});
