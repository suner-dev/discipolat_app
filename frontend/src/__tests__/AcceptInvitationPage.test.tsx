import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import AcceptInvitationPage from "@/pages/AcceptInvitationPage";
import api from "@/lib/api";

vi.mock("@/lib/api", () => ({
  default: { get: vi.fn(), post: vi.fn() },
}));

const mockGet = vi.mocked(api.get);
const mockPost = vi.mocked(api.post);

describe("AcceptInvitationPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
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
      <MemoryRouter initialEntries={["/accept-invitation?token=abc123"]}>
        <AcceptInvitationPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(mockGet).toHaveBeenCalledWith(
      "/admin/invitations/validate/abc123",
    ));
    expect(await screen.findByText(/Rejoindre Église Bethel/)).toBeTruthy();
    expect(mockPost).not.toHaveBeenCalled();
  });

  it("rejects a missing token and links to the actual login route", async () => {
    render(
      <MemoryRouter initialEntries={["/accept-invitation"]}>
        <AcceptInvitationPage />
      </MemoryRouter>,
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
      <MemoryRouter initialEntries={["/accept-invitation?token=abc123"]}>
        <AcceptInvitationPage />
      </MemoryRouter>,
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
      <MemoryRouter initialEntries={["/accept-invitation?token=abc123"]}>
        <AcceptInvitationPage />
      </MemoryRouter>,
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
});
