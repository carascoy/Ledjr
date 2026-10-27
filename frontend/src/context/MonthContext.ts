import { createContext, useContext } from "react";

// the month every signed-in page is showing, so switching pages keeps it.
// provider lives in components/MonthProvider.tsx (split so fast refresh doesn't complain)
export type MonthContextType = {
  month: Date; // always the 1st of the month
  setMonth: (month: Date) => void;
};

export const MonthContext = createContext<MonthContextType | undefined>(undefined);

export function useMonth() {
  const context = useContext(MonthContext);
  if (context === undefined) {
    throw new Error("useMonth must be used inside a MonthProvider");
  }
  return context;
}
