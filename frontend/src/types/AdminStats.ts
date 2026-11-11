export interface AdminStats {
  users: { total: number; today: number; last7Days: number; last30Days: number; addedData: number; activeLast7Days: number };
  demos: { total: number; today: number; last7Days: number; liveNow: number };
  days: { day: string; signups: number; demos: number }[];
  recentSignups: { firstName: string; joined: string; lastSeen: string | null; transactions: number }[];
  content: { transactions: number; recurring: number; debts: number; budgets: number };
  databaseSize: string;
}
