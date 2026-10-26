import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { DayPicker } from "react-day-picker";
import { fromInputDate, toInputDate } from "../utils/dates";
import "../styles/DatePicker.css";

type Props = {
  value: string; // "YYYY-MM-DD"
  onChange: (value: string) => void;
  max?: Date; // days after this are disabled
};

// Shows "Today", "Yesterday", or a short date like "Oct 22".
function labelFor(date: Date) {
  const today = new Date();
  const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1);
  if (toInputDate(date) === toInputDate(today)) return "Today";
  if (toInputDate(date) === toInputDate(yesterday)) return "Yesterday";
  return date.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

function DatePicker({ value, onChange, max }: Props) {
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState<React.CSSProperties>({});
  const triggerRef = useRef<HTMLButtonElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);

  const selected = fromInputDate(value);

  function openPicker() {
    const rect = triggerRef.current!.getBoundingClientRect();
    const popoverWidth = 340;
    const roomBelow = window.innerHeight - rect.bottom;
    const left = Math.min(rect.left, window.innerWidth - popoverWidth - 16);

    // Open downward if there's room, otherwise open upward above the field.
    setPosition(
      roomBelow > 400
        ? { top: rect.bottom + 8, left }
        : { bottom: window.innerHeight - rect.top + 8, left }
    );
    setOpen(true);
  }

  // While open: close on outside click, Escape, or scroll.
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

  return (
    <>
      <button
        ref={triggerRef}
        type="button"
        className="qa-input dp-trigger"
        onClick={() => (open ? setOpen(false) : openPicker())}
        aria-haspopup="dialog"
        aria-expanded={open}
      >
        {labelFor(selected)}
      </button>

      {/* Portal: render into <body> so the quick-add panel's overflow: hidden can't clip the calendar. */}
      {open &&
        createPortal(
          <div
            ref={popoverRef}
            className="dp-popover"
            style={position}
            role="dialog"
            aria-label="Choose date"
          >
            <DayPicker
              mode="single"
              required
              selected={selected}
              defaultMonth={selected}
              onSelect={(date) => {
                onChange(toInputDate(date));
                setOpen(false);
                triggerRef.current?.focus();
              }}
              disabled={max ? { after: max } : undefined}
              endMonth={max}
              showOutsideDays
              autoFocus
            />
          </div>,
          document.body
        )}
    </>
  );
}

export default DatePicker;
