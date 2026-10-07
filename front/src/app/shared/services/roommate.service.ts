import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RoommateSummary } from '../models/roommate-summary.model';

@Injectable({ providedIn: 'root' })
export class RoommateService {
  private readonly http = inject(HttpClient);

  getRoommates(): Observable<RoommateSummary[]> {
    return this.http.get<RoommateSummary[]>('/api/me/roommates');
  }
}