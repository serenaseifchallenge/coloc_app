import { Injectable, signal } from '@angular/core';
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

  private readonly apiUrl = 'http://localhost:8080/api/events';

  events = signal<CalendarEvent[]>([]);

  constructor(private http: HttpClient) {}

  getEvents(sharedHouseId: number): Observable<CalendarEvent[]> {
    return this.http
      .get<CalendarEvent[]>(
        `${this.apiUrl}?sharedHouseId=${sharedHouseId}`
      )
      .pipe(
        tap(events => this.events.set(events))
      );
  }

  getEvent(id: number): Observable<CalendarEvent> {
    return this.http.get<CalendarEvent>(
      `${this.apiUrl}/${id}`
    );
  }

  addEvent(
    event: CalendarEventRequest
  ): Observable<CalendarEvent> {

    return this.http
      .post<CalendarEvent>(
        this.apiUrl,
        event
      )
      .pipe(
        tap(createdEvent => {
          this.events.update(events => [
            ...events,
            createdEvent
          ]);
        })
      );
  }

  updateEvent(
    id: number,
    event: CalendarEventRequest
  ): Observable<CalendarEvent> {

    return this.http
      .put<CalendarEvent>(
        `${this.apiUrl}/${id}`,
        event
      )
      .pipe(
        tap(updatedEvent => {
          this.events.update(events =>
            events.map(event =>
              event.id === id
                ? updatedEvent
                : event
            )
          );
        })
      );
  }

  deleteEvent(id: number): Observable<void> {

    return this.http
      .delete<void>(
        `${this.apiUrl}/${id}`
      )
      .pipe(
        tap(() => {
          this.events.update(events =>
            events.filter(event => event.id !== id)
          );
        })
      );
  }
}