import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Balances,
  Expense,
  ExpenseRequest,
  ExpenseSummary,
  Reimbursement,
  ReimbursementRequest,
} from '../models/expense.models';

@Injectable({ providedIn: 'root' })
export class ExpenseService {
  private readonly http = inject(HttpClient);

  getExpenses(month: string): Observable<Expense[]> {
    return this.http.get<Expense[]>('/api/expenses', { params: { month } });
  }

  getSummary(month: string): Observable<ExpenseSummary> {
    return this.http.get<ExpenseSummary>('/api/expenses/summary', { params: { month } });
  }

  createExpense(request: ExpenseRequest): Observable<Expense> {
    return this.http.post<Expense>('/api/expenses', request);
  }

  updateExpense(id: number, request: ExpenseRequest): Observable<Expense> {
    return this.http.put<Expense>(`/api/expenses/${id}`, request);
  }

  deleteExpense(id: number): Observable<void> {
    return this.http.delete<void>(`/api/expenses/${id}`);
  }

  getBalances(): Observable<Balances> {
    return this.http.get<Balances>('/api/balances');
  }

  getReimbursements(limit = 10): Observable<Reimbursement[]> {
    return this.http.get<Reimbursement[]>('/api/reimbursements', { params: { limit } });
  }

  createReimbursement(request: ReimbursementRequest): Observable<Reimbursement> {
    return this.http.post<Reimbursement>('/api/reimbursements', request);
  }
}
