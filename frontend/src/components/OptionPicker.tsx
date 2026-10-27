import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import "../styles/CategoryPicker.css";

export type Option = {
  id: number;
  label: string;
  hint?: string; // gray text on the right, like a balance
};

type Props = {
  options: Option[];
  value: number | null;
  onChange: (id: number | null) => void;
  placeholder: string;
  noneLabel?: string; // if set, adds a "none" option at the top (value null)
  emptyMessage?: string;
  ariaLabel: string;
};

// dropdown that looks like the category picker, for "Paid with" and "Which debt?".
// portal like the other popovers so quick-add doesn't cut it off
function OptionPicker({ options, value, onChange, placeholder, noneLabel, emptyMessage, ariaLabel }: Props) {
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState<React.CSSProperties>({});
  const triggerRef = useRef<HTMLButtonElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);

  const selected = options.find((o) => o.id === value) ?? null;

  function openPicker() {
    const rect = triggerRef.current!.getBoundingClientRect();
    const width = 260;
    const left = Math.min(rect.left, window.innerWidth - width - 16);
    const roomBelow = window.innerHeight - rect.bottom;
    setPosition(
      roomBelow > 320
        ? { top: rect.bottom + 8, left, width }
        : { bottom: window.innerHeight - rect.top + 8, left, width }
    );
    setOpen(true);
  }

  function choose(id: number | null) {
    onChange(id);
    setOpen(false);
    triggerRef.current?.focus();
  }

  useEffect(() => {
    if (!open) return;
    function onPointerDown(e: MouseEvent) {
      const target = e.target as Node;
      if (!popoverRef.current?.contains(target) && !triggerRef.current?.contains(target)) setOpen(false);
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

  const triggerText = selected ? selected.label : value === null && noneLabel ? noneLabel : placeholder;
  const isPlaceholder = !selected && !(value === null && noneLabel);

  return (
    <>
      <button
        ref={triggerRef}
        type="button"
        className="qa-input cp-trigger"
        onClick={() => (open ? setOpen(false) : openPicker())}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label={ariaLabel}
      >
        <span className={isPlaceholder ? "cp-placeholder" : "op-value"}>{triggerText}</span>
      </button>

      {open &&
        createPortal(
          <div ref={popoverRef} className="cp-popover" style={position}>
            <div className="cp-list" role="listbox" aria-label={ariaLabel}>
              {noneLabel && (
                <button
                  type="button"
                  role="option"
                  aria-selected={value === null}
                  className={`cp-option ${value === null ? "is-selected" : ""}`}
                  onClick={() => choose(null)}
                >
                  <span className="cp-none">{noneLabel}</span>
                </button>
              )}
              {options.map((o) => (
                <button
                  key={o.id}
                  type="button"
                  role="option"
                  aria-selected={o.id === value}
                  className={`cp-option op-option ${o.id === value ? "is-selected" : ""}`}
                  onClick={() => choose(o.id)}
                >
                  <span className="op-label">{o.label}</span>
                  {o.hint && <span className="op-hint">{o.hint}</span>}
                </button>
              ))}
              {options.length === 0 && emptyMessage && <p className="op-empty">{emptyMessage}</p>}
            </div>
          </div>,
          document.body
        )}
    </>
  );
}

export default OptionPicker;
