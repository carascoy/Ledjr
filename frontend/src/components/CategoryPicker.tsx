import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useUser } from "../context/UserContext";
import type { CategoryResponse } from "../types/CategoryResponse";
import CategoryTag from "./CategoryTag";
import "../styles/CategoryPicker.css";

type Props = {
  categories: CategoryResponse[] | null; // null = still loading
  kind: "income" | "expense"; // only categories of this kind are offered
  value: number | null; // selected category id, or null for "No category"
  onChange: (id: number | null) => void;
  onCategoriesChanged: (categories: CategoryResponse[]) => void; // after creating / seeding
};

const API = "http://localhost:8080";

function CategoryPicker({ categories, kind, value, onChange, onCategoriesChanged }: Props) {
  const { user } = useUser();
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState<React.CSSProperties>({});
  const [newName, setNewName] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);

  const options = (categories ?? []).filter((c) => c.kind === kind);
  const selected = (categories ?? []).find((c) => c.id === value) ?? null;

  function openPicker() {
    const rect = triggerRef.current!.getBoundingClientRect();
    const width = 280;
    const left = Math.min(rect.left, window.innerWidth - width - 16);
    const roomBelow = window.innerHeight - rect.bottom;
    setPosition(
      roomBelow > 380
        ? { top: rect.bottom + 8, left }
        : { bottom: window.innerHeight - rect.top + 8, left }
    );
    setError("");
    setOpen(true);
  }

  function choose(id: number | null) {
    onChange(id);
    setOpen(false);
    triggerRef.current?.focus();
  }

  // While open: close on outside click, Escape, or scroll (same behavior as the date picker).
  useEffect(() => {
    if (!open) return;
    function onPointerDown(e: MouseEvent) {
      const target = e.target as Node;
      if (!popoverRef.current?.contains(target) && !triggerRef.current?.contains(target)) {
        setOpen(false);
      }
    }
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") {
        setOpen(false);
        triggerRef.current?.focus();
      }
    }
    function onScroll() {
      setOpen(false);
    }
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
      window.removeEventListener("scroll", onScroll);
    };
  }, [open]);

  async function createCategory(e: React.FormEvent) {
    e.preventDefault();
    // This form is rendered through a portal, so in the DOM it sits in <body>, but React still
    // bubbles its events up the *component* tree, straight into QuickAdd's <form onSubmit>.
    // Without this line, adding a category also submitted the whole transaction.
    e.stopPropagation();
    if (!user || !newName.trim()) return;
    setBusy(true);
    setError("");
    try {
      const response = await fetch(`${API}/categories`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId: user.id, name: newName.trim(), kind }),
      });
      if (!response.ok) {
        setError(await response.text());
        return;
      }
      const created: CategoryResponse = await response.json();
      onCategoriesChanged([...(categories ?? []), created].sort((a, b) => a.name.localeCompare(b.name)));
      setNewName("");
      choose(created.id);
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setBusy(false);
    }
  }

  async function addStarters() {
    if (!user) return;
    setBusy(true);
    setError("");
    try {
      const response = await fetch(`${API}/categories/defaults?userId=${user.id}`, { method: "POST" });
      if (!response.ok) {
        setError(await response.text());
        return;
      }
      onCategoriesChanged(await response.json());
    } catch {
      setError("Couldn't reach the server.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <button
        ref={triggerRef}
        type="button"
        className="qa-input cp-trigger"
        onClick={() => (open ? setOpen(false) : openPicker())}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        {selected ? (
          <CategoryTag name={selected.name} colorSlot={selected.colorSlot} />
        ) : (
          <span className="cp-placeholder">Category</span>
        )}
      </button>

      {/* Portal: render into <body> so the quick-add panel's overflow: hidden can't clip it. */}
      {open &&
        createPortal(
          <div ref={popoverRef} className="cp-popover" style={position}>
            <div className="cp-list" role="listbox" aria-label={`${kind} categories`}>
              <button
                type="button"
                role="option"
                aria-selected={value === null}
                className={`cp-option ${value === null ? "is-selected" : ""}`}
                onClick={() => choose(null)}
              >
                <span className="cp-none">No category</span>
              </button>

              {options.map((c) => (
                <button
                  key={c.id}
                  type="button"
                  role="option"
                  aria-selected={c.id === value}
                  className={`cp-option ${c.id === value ? "is-selected" : ""}`}
                  onClick={() => choose(c.id)}
                >
                  <CategoryTag name={c.name} colorSlot={c.colorSlot} />
                </button>
              ))}

              {categories !== null && categories.length === 0 && (
                <button type="button" className="cp-starters" onClick={addStarters} disabled={busy}>
                  + Add starter categories
                </button>
              )}
            </div>

            <form className="cp-new" onSubmit={createCategory}>
              <input
                className="cp-new-input"
                placeholder={`New ${kind} category`}
                value={newName}
                maxLength={40}
                onChange={(e) => setNewName(e.target.value)}
              />
              <button type="submit" className="cp-new-add" disabled={busy || !newName.trim()}>
                Add
              </button>
            </form>

            {error && <p className="cp-error">{error}</p>}
          </div>,
          document.body
        )}
    </>
  );
}

export default CategoryPicker;
