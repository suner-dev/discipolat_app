import { useCallback, useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { useI18n } from "@/i18n";
import api, { getErrorMessage } from "@/lib/api";
import toast from "react-hot-toast";
import { Loader2, Search, UserPlus, UsersRound, X } from "lucide-react";

/**
 * §G3.1/§G6.4 — Répertoire des personnes (backend /api/v1/people, seul source
 * de vérité) : recherche, filtres RÉELS « sans espace » / « sans famille » et
 * affectation à un espace par un responsable — le serveur notifie la personne
 * (« Vous avez été ajouté à [Espace] par [X] »).
 */

interface Person {
  id: string;
  firstName: string;
  lastName: string;
  fullName?: string;
  emailNormalized?: string;
  phoneNormalized?: string;
  status?: string;
}

interface SpaceOption {
  id: string;
  name: string;
}

export default function PeopleDirectoryPage() {
  const { t } = useI18n();
  const { user } = useAuth();
  // §G6.4 — liens profonds /people?withoutSpace=true&withoutFamily=true
  const [searchParams] = useSearchParams();
  const [people, setPeople] = useState<Person[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [withoutSpace, setWithoutSpace] = useState(searchParams.get("withoutSpace") === "true");
  const [withoutFamily, setWithoutFamily] = useState(searchParams.get("withoutFamily") === "true");
  const [loading, setLoading] = useState(true);
  const [assignTarget, setAssignTarget] = useState<Person | null>(null);
  const [spaces, setSpaces] = useState<SpaceOption[]>([]);
  const [selectedSpace, setSelectedSpace] = useState("");
  const [busy, setBusy] = useState(false);
  const [registerOpen, setRegisterOpen] = useState(false);
  const [form, setForm] = useState({ firstName: "", lastName: "", email: "", phone: "" });

  const canRegister = user?.role === "ADMIN" || user?.role === "PASTEUR";

  const fetchPeople = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.set("page", page.toString());
      params.set("size", "20");
      if (appliedSearch) params.set("search", appliedSearch);
      if (withoutSpace) params.set("withoutSpace", "true");
      if (withoutFamily) params.set("withoutFamily", "true");
      const res = await api.get(`/people?${params.toString()}`);
      setPeople(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
      setTotalElements(res.data.totalElements || 0);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [page, appliedSearch, withoutSpace, withoutFamily]);

  useEffect(() => {
    fetchPeople();
  }, [fetchPeople]);

  const openAssign = async (person: Person) => {
    setAssignTarget(person);
    setSelectedSpace("");
    try {
      const res = await api.get("/spaces");
      setSpaces((res.data || []).map((s: SpaceOption) => ({ id: s.id, name: s.name })));
    } catch {
      setSpaces([]);
    }
  };

  const confirmAssign = async () => {
    if (!assignTarget || !selectedSpace) return;
    setBusy(true);
    try {
      await api.post(`/people/${assignTarget.id}/spaces`, null, {
        params: { spaceId: selectedSpace },
      });
      toast.success(t("people.assigned"));
      setAssignTarget(null);
      fetchPeople();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const registerPerson = async () => {
    if (!form.firstName.trim() || !form.lastName.trim()) {
      toast.error(t("people.nameRequired"));
      return;
    }
    setBusy(true);
    try {
      await api.post("/people/register", {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        emailNormalized: form.email.trim() || undefined,
        phoneNormalized: form.phone.trim() || undefined,
      }, { params: { source: "MANUEL" } });
      toast.success(t("people.registered"));
      setRegisterOpen(false);
      setForm({ firstName: "", lastName: "", email: "", phone: "" });
      fetchPeople();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const label = (p: Person) => p.fullName || `${p.firstName} ${p.lastName}`;

  return (
    <div className="p-4 md:p-6 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold flex items-center gap-2">
            <UsersRound className="w-5 h-5" /> {t("people.title")}
          </h1>
          <p className="text-sm opacity-70">{t("people.subtitle")}</p>
        </div>
        {canRegister && (
          <button className="btn btn-primary btn-sm flex items-center gap-2" onClick={() => setRegisterOpen(true)}>
            <UserPlus className="w-4 h-4" /> {t("people.register")}
          </button>
        )}
      </div>

      <div className="card p-3 flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 opacity-60" />
          <input
            className="input input-sm w-full pl-9"
            placeholder={t("people.search")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                setPage(0);
                setAppliedSearch(search.trim());
              }
            }}
          />
        </div>
        <label className="label cursor-pointer gap-2 text-sm">
          <input
            type="checkbox"
            className="checkbox checkbox-sm"
            checked={withoutSpace}
            onChange={(e) => {
              setPage(0);
              setWithoutSpace(e.target.checked);
            }}
          />
          {t("people.filterWithoutSpace")}
        </label>
        <label className="label cursor-pointer gap-2 text-sm">
          <input
            type="checkbox"
            className="checkbox checkbox-sm"
            checked={withoutFamily}
            onChange={(e) => {
              setPage(0);
              setWithoutFamily(e.target.checked);
            }}
          />
          {t("people.filterWithoutFamily")}
        </label>
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => {
            setSearch("");
            setAppliedSearch("");
            setPage(0);
            fetchPeople();
          }}
        >
          {t("people.reset")}
        </button>
      </div>

      <div className="card overflow-x-auto">
        {loading ? (
          <div className="p-8 flex justify-center">
            <Loader2 className="w-6 h-6 animate-spin" />
          </div>
        ) : people.length === 0 ? (
          <div className="p-8 text-center opacity-70">{t("people.empty")}</div>
        ) : (
          <table className="table w-full">
            <thead>
              <tr>
                <th>{t("people.fullName")}</th>
                <th>{t("people.email")}</th>
                <th>{t("people.phone")}</th>
                <th>{t("people.status")}</th>
                <th className="text-right">{t("people.actions")}</th>
              </tr>
            </thead>
            <tbody>
              {people.map((p) => (
                <tr key={p.id} className="hover">
                  <td className="font-medium">{label(p)}</td>
                  <td>{p.emailNormalized || "—"}</td>
                  <td>{p.phoneNormalized || "—"}</td>
                  <td>
                    <span className="badge badge-sm badge-outline">{p.status || "ACTIVE"}</span>
                  </td>
                  <td className="text-right">
                    <button className="btn btn-xs btn-primary" onClick={() => openAssign(p)}>
                      {t("people.assign")}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-between text-sm">
          <span>
            {t("people.count").replace("{n}", String(totalElements))}
          </span>
          <div className="flex gap-2">
            <button className="btn btn-xs" disabled={page === 0} onClick={() => setPage(page - 1)}>
              ←
            </button>
            <span className="px-2 py-1">
              {page + 1} / {totalPages}
            </span>
            <button className="btn btn-xs" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>
              →
            </button>
          </div>
        </div>
      )}

      {/* Modale d'affectation */}
      {assignTarget && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" onClick={() => setAssignTarget(null)}>
          <div className="card bg-base-1xl w-full max-w-md p-4 space-y-3" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between">
              <h3 className="font-semibold">{t("people.assignTitle")}</h3>
              <button className="btn btn-ghost btn-xs" onClick={() => setAssignTarget(null)}>
                <X className="w-4 h-4" />
              </button>
            </div>
            <p className="text-sm opacity-80">{label(assignTarget)}</p>
            <select
              className="select select-sm w-full"
              value={selectedSpace}
              onChange={(e) => setSelectedSpace(e.target.value)}
            >
              <option value="">{t("people.selectSpace")}</option>
              {spaces.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
            <div className="flex justify-end gap-2">
              <button className="btn btn-ghost btn-sm" onClick={() => setAssignTarget(null)}>
                {t("people.cancel")}
              </button>
              <button className="btn btn-primary btn-sm" disabled={!selectedSpace || busy} onClick={confirmAssign}>
                {busy ? <Loader2 className="w-4 h-4 animate-spin" /> : t("people.confirmAssign")}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modale d'enregistrement (ADMIN/PASTEUR) */}
      {registerOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" onClick={() => setRegisterOpen(false)}>
          <div className="card bg-base-1xl w-full max-w-md p-4 space-y-3" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between">
              <h3 className="font-semibold">{t("people.registerTitle")}</h3>
              <button className="btn btn-ghost btn-xs" onClick={() => setRegisterOpen(false)}>
                <X className="w-4 h-4" />
              </button>
            </div>
            <input
              className="input input-sm w-full"
              placeholder={t("people.firstName")}
              value={form.firstName}
              onChange={(e) => setForm({ ...form, firstName: e.target.value })}
            />
            <input
              className="input input-sm w-full"
              placeholder={t("people.lastName")}
              value={form.lastName}
              onChange={(e) => setForm({ ...form, lastName: e.target.value })}
            />
            <input
              className="input input-sm w-full"
              placeholder={t("people.email")}
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
            <input
              className="input input-sm w-full"
              placeholder={t("people.phone")}
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
            <div className="flex justify-end gap-2">
              <button className="btn btn-ghost btn-sm" onClick={() => setRegisterOpen(false)}>
                {t("people.cancel")}
              </button>
              <button className="btn btn-primary btn-sm" disabled={busy} onClick={registerPerson}>
                {busy ? <Loader2 className="w-4 h-4 animate-spin" /> : t("people.register")}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
