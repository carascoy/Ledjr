export interface Transaction{
    id:number;
    userId:number;
    categoryId:number;
    debtId?:number;
    type?:string;
    amount:number;
    txnDate?:string;
    description?:string;
}