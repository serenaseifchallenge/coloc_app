import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { LastMonthResults } from '../models/points.model';

@Injectable({ providedIn: 'root' })
export class PointsService {
  private readonly http = inject(HttpClient);

  getLastMonthResults(): Observable<LastMonthResults> {
    return this.http.get<LastMonthResults>('/api/points/last-month');
  }
}