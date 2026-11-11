export interface AppUser{
    id:number;
    email:string;
    firstName:string;
    lastName:string;
    createdAt?:string;
    setupDone?:boolean; // false = they haven't finished or skipped setup yet
    demo?:boolean;      // a "Try the demo" account: fake data, deleted after 24 hours
}
