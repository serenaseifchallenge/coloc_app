import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, firstValueFrom, tap } from 'rxjs';
import { SharedHouse, SharedHouseRequest } from '../../shared/models/shared-house.model';

@Injectable({ providedIn: 'root' })
export class SharedHouseService {
  private readonly http = inject(HttpClient);
  private readonly api = '/api/shared-houses';
  private loaded = false;

  /** La coloc de l'utilisateur connecté (null = pas de coloc). */
  readonly house = signal<SharedHouse | null>(null);

  /** Charge la coloc une seule fois, sauf si force = true. */
  async load(force = false): Promise<SharedHouse | null> {
    if (this.loaded && !force) {
      return this.house();
    }
    try {
      this.house.set(await firstValueFrom(this.http.get<SharedHouse>(`${this.api}/mine`)));
    } catch (error) {
      // 403 = l'utilisateur n'a pas encore de coloc
      if (error instanceof HttpErrorResponse && error.status === 403) {
        this.house.set(null);
      } else {
        throw error;
      }
    }
    this.loaded = true;
    return this.house();
  }

  create(body: SharedHouseRequest): Observable<SharedHouse> {
    return this.http.post<SharedHouse>(this.api, body).pipe(tap((house) => this.store(house)));
  }

  update(body: SharedHouseRequest): Observable<SharedHouse> {
    return this.http.put<SharedHouse>(`${this.api}/mine`, body).pipe(tap((house) => this.store(house)));
  }

  join(invitationCode: string): Observable<SharedHouse> {
    return this.http
      .post<SharedHouse>(`${this.api}/join`, { invitationCode })
      .pipe(tap((house) => this.store(house)));
  }

  leave(): Observable<void> {
    return this.http.post<void>(`${this.api}/leave`, {}).pipe(tap(() => this.store(null)));
  }

  /** Oublie la coloc en mémoire (à la déconnexion ou au changement de compte). */
  reset(): void {
    this.house.set(null);
    this.loaded = false;
  }

  private store(house: SharedHouse | null): void {
    this.house.set(house);
    this.loaded = true;
  }
}
