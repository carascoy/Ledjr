import { useState } from "react";
import type { ReactNode } from "react";
import { MonthContext } from "../context/MonthContext";
import { startOfThisMonth } from "../utils/months";

// sits in AppLayout, which stays mounted while you move between pages,
// so the month survives going dashboard -> transactions and back
function MonthProvider({ children }: { children: ReactNode }) {
  const [month, setMonth] = useState(startOfThisMonth);
  return <MonthContext.Provider value={{ month, setMonth }}>{children}</MonthContext.Provider>;
}

export default MonthProvider;
