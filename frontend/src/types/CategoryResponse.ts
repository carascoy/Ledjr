// matches the backend CategoryResponse DTO (GET/POST /categories)
export interface CategoryResponse {
  id: number;
  name: string;
  kind: "income" | "expense";
  colorSlot: number; // 0-7, see utils/categoryColors.ts
}
