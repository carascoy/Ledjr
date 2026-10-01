
export type Frequency = "daily" | "weekly" | "biweekly" | "monthly" | "annually";

export interface RecurringTemplate{
    id:number;
    userId:number;
    categoryId:number;
    name:string;
    amount:number;
    frequency?:Frequency;
    nextDueDate?:string;
}