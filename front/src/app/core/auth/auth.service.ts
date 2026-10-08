import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { EMPTY, Observable, catchError, firstValueFrom, tap } from 'rxjs';
import { Roommate } from '../../shared/models/roommate.model';
import { AuthResponse, LoginRequest, RegisterRequest, UpdateCredentialsRequest } from './auth.model';
import { SharedHouseService } from '../shared-house/shared-house.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private static readonly TOKEN_KEY = 'coloc_token';

  private readonly http = inject(HttpClient);
  private readonly houseService = inject(SharedHouseService);

  readonly token = signal<string | null>(AuthService.readToken());
  readonly user = signal<Roommate | null>(null);
  readonly isLoggedIn = computed(() => this.token() !== null);

  login(body: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', body).pipe(tap((res) => this.startSession(res)));
  }

  register(body: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', body).pipe(tap((res) => this.startSession(res)));
  }

  /** Charge l'utilisateur connecté (après un rechargement de page par exemple). */
  async loadUser(): Promise<Roommate | null> {
    if (!this.token()) {
      return null;
    }
    if (this.user()) {
      return this.user();
    }
    try {
      const me = await firstValueFrom(this.http.get<Roommate>('/api/me'));
      this.user.set(me);
      return me;
    } catch {
      this.logout();
      return null;
    }
  }

  /** Relit l'utilisateur connecté depuis le back (par exemple après avoir gagné des points). */
  refreshUser(): Observable<Roommate> {
    return this.http.get<Roommate>('/api/me').pipe(
      tap((me) => this.user.set(me)),
      catchError(() => EMPTY),
    );
  }

  updateCredentials(body: UpdateCredentialsRequest): Observable<Roommate> {
    return this.http.patch<Roommate>('/api/me/credentials', body).pipe(tap((me) => this.user.set(me)));
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>('/api/me').pipe(tap(() => this.logout()));
  }

  logout(): void {
    try {
      localStorage.removeItem(AuthService.TOKEN_KEY);
    } catch {
      /* stockage indisponible : rien à effacer */
    }
    this.token.set(null);
    this.user.set(null);
    this.houseService.reset();
  }

  private startSession(res: AuthResponse): void {
    try {
      localStorage.setItem(AuthService.TOKEN_KEY, res.token);
    } catch {
      /* stockage indisponible : la session durera jusqu'au rechargement */
    }
    this.token.set(res.token);
    this.user.set(res.roommate);
    this.houseService.reset();
  }

  private static readToken(): string | null {
    try {
      return localStorage.getItem(AuthService.TOKEN_KEY);
    } catch {
      return null;
    }
  }
}
