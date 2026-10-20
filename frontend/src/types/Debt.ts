export interface Debt{
    id:number;
    userId: number;
    creditorName: string;
    balance:number;
    originalBalance:number;
    apr?:number;
    minPayment?:number;
    dueDate?:number;
    priorityRank?:number;
}