// one expense category's row. budgeted = null means no budget this month
export type BudgetLine = {
  categoryId: number;
  name: string;
  colorSlot: number;
  budgeted: number | null;
  spent: number;
  suggested: number | null; // null = nothing to go off of yet
  suggestionReason: "bill" | "average" | null;
};

// what GET /budget?month=2026-10 sends back (BudgetResponse.java)
export type BudgetResponse = {
  month: string;
  lines: BudgetLine[]; // budgeted ones first
  totalBudgeted: number;
  totalSpent: number; // only in budgeted categories
  unbudgetedSpent: number; // no-budget categories + uncategorized
  income: number; // logged this month
  savedTotal: number; // unspent budget from every finished month
  canCopyLastMonth: boolean;

  // the "how much can I budget" estimate
  expectedIncome: number;
  incomeBasis: "recurring" | "logged" | "last-month" | "none";
  paycheckCount: number;
  debtMinimums: number;
  otherBills: number; // recurring bills with no category
  canBudget: number; // expectedIncome - debtMinimums - otherBills
};
