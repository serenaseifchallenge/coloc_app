import { Injectable, signal } from '@angular/core';
import { CalendarEvent } from './calendar-event.model';

@Injectable({
  providedIn: 'root'
})
export class CalendarService {

  private readonly eventsSignal = signal<CalendarEvent[]>([
    {
      id: 1,
      title: 'Dîner entre colocataires',
      description: 'Dîner tous ensemble dans la cuisine.',
      startDate: '2026-10-07',
      endDate: '2026-10-07',
      allDay: false,
      startTime: '19:00',
      endTime: '21:00',
      creatorName: 'Mila A',
      creatorInitials: 'MA'
    },
    {
      id: 2,
      title: 'Réunion de coloc',
      description: 'Discussion concernant la vie de la colocation.',
      startDate: '2026-10-08',
      endDate: '2026-10-08',
      allDay: false,
      startTime: '18:00',
      endTime: '19:00',
      creatorName: 'Emma Lehec',
      creatorInitials: 'EL'
    },
    {
      id: 3,
      title: 'Anniversaire',
      description: 'Anniversaire de la colocataire.',
      startDate: '2026-10-12',
      endDate: '2026-10-12',
      allDay: true,
      startTime: '',
      endTime: '',
      creatorName: 'Mila A',
      creatorInitials: 'MA'
    },
    {
      id: 4,
      title: 'Week-end entre colocataires',
      description: 'Week-end tous ensemble.',
      startDate: '2026-10-16',
      endDate: '2026-10-18',
      allDay: true,
      startTime: '',
      endTime: '',
      creatorName: 'Emma Lehec',
      creatorInitials: 'EL'
    }
  ]);

  readonly events = this.eventsSignal.asReadonly();

  addEvent(event: Omit<CalendarEvent, 'id'>): void {
    const newEvent: CalendarEvent = {
      ...event,
      id: this.getNextId()
    };

    this.eventsSignal.update(events => [
      ...events,
      newEvent
    ]);
  }

  updateEvent(updatedEvent: CalendarEvent): void {
    this.eventsSignal.update(events =>
      events.map(event =>
        event.id === updatedEvent.id
          ? updatedEvent
          : event
      )
    );
  }

  deleteEvent(id: number): void {
    this.eventsSignal.update(events =>
      events.filter(event => event.id !== id)
    );
  }

  private getNextId(): number {
    const events = this.eventsSignal();

    if (events.length === 0) {
      return 1;
    }

    return Math.max(
      ...events.map(event => event.id)
    ) + 1;
  }
}