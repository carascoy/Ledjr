
export type Kind = "income" | "expense" | "debt";

export interface Category{
    id:number;
    userId:number;
    name:string;
    kind:Kind;
    budgetedAmount?:number;

}