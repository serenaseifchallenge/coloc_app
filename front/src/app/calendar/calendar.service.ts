import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import {
  CalendarEvent,
  CalendarEventRequest
} from './calendar-event.model';

@Injectable({
  providedIn: 'root'
})
export class CalendarService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/events';

  readonly events = signal<CalendarEvent[]>([]);

  getEvents(): Observable<CalendarEvent[]> {
    return this.http
      .get<CalendarEvent[]>(this.apiUrl)
      .pipe(
        tap(events => this.events.set(events))
      );
  }

  getEvent(id: number): Observable<CalendarEvent> {
    return this.http.get<CalendarEvent>(`${this.apiUrl}/${id}`);
  }

  addEvent(request: CalendarEventRequest): Observable<CalendarEvent> {
    return this.http
      .post<CalendarEvent>(this.apiUrl, request)
      .pipe(
        tap(createdEvent => {
          this.events.update(events => [...events, createdEvent]);
        })
      );
  }

  updateEvent(
    id: number,
    request: CalendarEventRequest
  ): Observable<CalendarEvent> {
    return this.http
      .put<CalendarEvent>(`${this.apiUrl}/${id}`, request)
      .pipe(
        tap(updatedEvent => {
          this.events.update(events =>
            events.map(event =>
              event.id === id ? updatedEvent : event
            )
          );
        })
      );
  }

  deleteEvent(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.apiUrl}/${id}`)
      .pipe(
        tap(() => {
          this.events.update(events =>
            events.filter(event => event.id !== id)
          );
        })
      );
  }

  clearEvents(): void {
    this.events.set([]);
  }
}