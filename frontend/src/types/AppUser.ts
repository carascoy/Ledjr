export interface AppUser{
    id:number;
    email:string;
    firstName:string;
    lastName:string;
    createdAt?:string;
    setupDone?:boolean; // false = they haven't finished or skipped setup yet
}
