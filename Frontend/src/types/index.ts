export enum CategoryType {
  Expense = 1,
  Income = 2
}

export interface Category {
  id: string;
  name: string;
  type: CategoryType;
  createdAt: string;
}

export interface Transaction {
  id: string;
  amount: number;
  categoryId: string;
  comment?: string;
  date: string;
  category?: Category;
}