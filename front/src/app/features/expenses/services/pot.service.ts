import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Pot, PotPaymentRequest, PotRequest } from '../models/expense.models';

@Injectable({ providedIn: 'root' })
export class PotService {
  private readonly http = inject(HttpClient);

  getPots(): Observable<Pot[]> {
    return this.http.get<Pot[]>('/api/pots');
  }

  createPot(request: PotRequest): Observable<Pot> {
    return this.http.post<Pot>('/api/pots', request);
  }

  updatePot(id: number, request: PotRequest): Observable<Pot> {
    return this.http.put<Pot>(`/api/pots/${id}`, request);
  }

  deletePot(id: number): Observable<void> {
    return this.http.delete<void>(`/api/pots/${id}`);
  }

  addPayment(id: number, request: PotPaymentRequest): Observable<Pot> {
    return this.http.post<Pot>(`/api/pots/${id}/payments`, request);
  }
}
