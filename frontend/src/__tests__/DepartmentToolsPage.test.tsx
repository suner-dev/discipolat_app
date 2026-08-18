import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DepartmentToolsPage from '@/pages/DepartmentToolsPage';

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'dept-1' }),
  };
});

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/departments/dept-1/tools']}>
        <Routes>
          <Route path="/departments/:id/tools" element={<DepartmentToolsPage />} />
          <Route path="/departments/:id/manage" element={<div>MANAGE PAGE</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('DepartmentToolsPage — redirection vers /manage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
  });

  it('redirige vers /departments/:id/manage', async () => {
    renderPage();
    expect(await screen.findByText('MANAGE PAGE')).toBeInTheDocument();
  });
});
