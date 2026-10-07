import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CalendarService } from './calendar.service';
import { CalendarEvent } from './calendar-event.model';

type CalendarView = 'month' | 'week';

@Component({
  selector: 'app-calendar',
  imports: [FormsModule],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.css'
})
export class CalendarComponent {

  currentDate = new Date();

  selectedDate = new Date();

  view: CalendarView = 'month';

  showEventForm = false;

  editingEventId: number | null = null;

  newEvent = {
    title: '',
    startDate: this.formatDate(this.selectedDate),
    endDate: this.formatDate(this.selectedDate),
    allDay: false,
    startTime: '18:00',
    endTime: '19:00',
    description: ''
  };

  readonly weekDays = [
    'DIM',
    'LUN',
    'MAR',
    'MER',
    'JEU',
    'VEN',
    'SAM'
  ];

  readonly monthNames = [
    'Janvier',
    'Février',
    'Mars',
    'Avril',
    'Mai',
    'Juin',
    'Juillet',
    'Août',
    'Septembre',
    'Octobre',
    'Novembre',
    'Décembre'
  ];

  constructor(
    public calendarService: CalendarService
  ) {}

  get currentMonthLabel(): string {
    return `${this.monthNames[this.currentDate.getMonth()]} ${this.currentDate.getFullYear()}`;
  }

  get selectedDateLabel(): string {
    return new Intl.DateTimeFormat('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }).format(this.selectedDate);
  }

  get modalTitle(): string {
    return this.editingEventId === null
      ? 'Nouvel événement'
      : 'Modifier l’événement';
  }

  get calendarDays(): Date[] {
    const year = this.currentDate.getFullYear();
    const month = this.currentDate.getMonth();

    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);

    const days: Date[] = [];

    const previousMonthDays = firstDay.getDay();

    for (let i = previousMonthDays - 1; i >= 0; i--) {
      days.push(new Date(year, month, -i));
    }

    for (let day = 1; day <= lastDay.getDate(); day++) {
      days.push(new Date(year, month, day));
    }

    const remainingDays = 42 - days.length;

    for (let day = 1; day <= remainingDays; day++) {
      days.push(new Date(year, month + 1, day));
    }

    return days;
  }

  get weekDaysDates(): Date[] {
    const date = new Date(this.selectedDate);
    const dayOfWeek = date.getDay();

    const sunday = new Date(date);
    sunday.setDate(date.getDate() - dayOfWeek);

    const days: Date[] = [];

    for (let i = 0; i < 7; i++) {
      const day = new Date(sunday);
      day.setDate(sunday.getDate() + i);
      days.push(day);
    }

    return days;
  }

  get selectedDayEvents(): CalendarEvent[] {
    return this.getEventsForDate(this.selectedDate)
      .sort((a, b) => {
        if (a.allDay && !b.allDay) {
          return -1;
        }

        if (!a.allDay && b.allDay) {
          return 1;
        }

        return a.startTime.localeCompare(b.startTime);
      });
  }

  getEventsForDate(date: Date): CalendarEvent[] {
    const dateString = this.formatDate(date);

    return this.calendarService
      .events()
      .filter(event => {
        return dateString >= event.startDate
          && dateString <= event.endDate;
      });
  }

  selectDate(date: Date): void {
    this.selectedDate = new Date(date);

    this.newEvent.startDate = this.formatDate(this.selectedDate);
    this.newEvent.endDate = this.formatDate(this.selectedDate);
  }

  previousMonth(): void {
    this.currentDate = new Date(
      this.currentDate.getFullYear(),
      this.currentDate.getMonth() - 1,
      1
    );
  }

  nextMonth(): void {
    this.currentDate = new Date(
      this.currentDate.getFullYear(),
      this.currentDate.getMonth() + 1,
      1
    );
  }

  previousWeek(): void {
    const date = new Date(this.selectedDate);

    date.setDate(date.getDate() - 7);

    this.selectedDate = date;
    this.currentDate = new Date(date);

    this.newEvent.startDate = this.formatDate(date);
    this.newEvent.endDate = this.formatDate(date);
  }

  nextWeek(): void {
    const date = new Date(this.selectedDate);

    date.setDate(date.getDate() + 7);

    this.selectedDate = date;
    this.currentDate = new Date(date);

    this.newEvent.startDate = this.formatDate(date);
    this.newEvent.endDate = this.formatDate(date);
  }

  goToToday(): void {
    const today = new Date();

    this.currentDate = new Date(today);
    this.selectedDate = new Date(today);

    this.newEvent.startDate = this.formatDate(today);
    this.newEvent.endDate = this.formatDate(today);
  }

  setView(view: CalendarView): void {
    this.view = view;
  }

  openEventForm(): void {
    this.editingEventId = null;

    const selectedDate = this.formatDate(this.selectedDate);

    this.newEvent = {
      title: '',
      startDate: selectedDate,
      endDate: selectedDate,
      allDay: false,
      startTime: '18:00',
      endTime: '19:00',
      description: ''
    };

    this.showEventForm = true;
  }

  openEditEvent(event: CalendarEvent): void {
    this.editingEventId = event.id;

    this.newEvent = {
      title: event.title,
      startDate: event.startDate,
      endDate: event.endDate,
      allDay: event.allDay,
      startTime: event.startTime,
      endTime: event.endTime,
      description: event.description
    };

    this.showEventForm = true;
  }

  closeEventForm(): void {
    this.showEventForm = false;
    this.editingEventId = null;
  }

  createOrUpdateEvent(): void {

    if (!this.newEvent.title.trim()) {
      return;
    }

    if (!this.newEvent.startDate) {
      return;
    }

    if (!this.newEvent.endDate) {
      return;
    }

    if (this.newEvent.endDate < this.newEvent.startDate) {
      alert('La date de fin doit être après ou égale à la date de début.');
      return;
    }

    if (
      !this.newEvent.allDay
      && this.newEvent.endDate === this.newEvent.startDate
      && this.newEvent.endTime <= this.newEvent.startTime
    ) {
      alert('L’heure de fin doit être après l’heure de début.');
      return;
    }

    if (this.editingEventId === null) {

      this.calendarService.addEvent({
        title: this.newEvent.title.trim(),
        description: this.newEvent.description.trim(),
        startDate: this.newEvent.startDate,
        endDate: this.newEvent.endDate,
        allDay: this.newEvent.allDay,
        startTime: this.newEvent.allDay
          ? ''
          : this.newEvent.startTime,
        endTime: this.newEvent.allDay
          ? ''
          : this.newEvent.endTime,
        creatorName: 'Mila A',
        creatorInitials: 'MA'
      });

    } else {

      const existingEvent = this.calendarService
        .events()
        .find(event => event.id === this.editingEventId);

      if (!existingEvent) {
        return;
      }

      const updatedEvent: CalendarEvent = {
        ...existingEvent,
        title: this.newEvent.title.trim(),
        description: this.newEvent.description.trim(),
        startDate: this.newEvent.startDate,
        endDate: this.newEvent.endDate,
        allDay: this.newEvent.allDay,
        startTime: this.newEvent.allDay
          ? ''
          : this.newEvent.startTime,
        endTime: this.newEvent.allDay
          ? ''
          : this.newEvent.endTime
      };

      this.calendarService.updateEvent(updatedEvent);
    }

    this.selectedDate = this.parseDate(this.newEvent.startDate);
    this.currentDate = new Date(this.selectedDate);

    this.closeEventForm();
  }

  deleteEvent(event: CalendarEvent): void {
    const confirmation = confirm(
      `Voulez-vous supprimer l’événement "${event.title}" ?`
    );

    if (confirmation) {
      this.calendarService.deleteEvent(event.id);
    }
  }

  isToday(date: Date): boolean {
    const today = new Date();

    return this.isSameDate(date, today);
  }

  isSelected(date: Date): boolean {
    return this.isSameDate(date, this.selectedDate);
  }

  isCurrentMonth(date: Date): boolean {
    return date.getMonth() === this.currentDate.getMonth()
      && date.getFullYear() === this.currentDate.getFullYear();
  }

  formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  formatDayNumber(date: Date): number {
    return date.getDate();
  }

  formatEventDate(event: CalendarEvent): string {
    const start = this.parseDate(event.startDate);
    const end = this.parseDate(event.endDate);

    const startText = new Intl.DateTimeFormat('fr-FR', {
      day: 'numeric',
      month: 'short'
    }).format(start);

    const endText = new Intl.DateTimeFormat('fr-FR', {
      day: 'numeric',
      month: 'short'
    }).format(end);

    if (event.startDate === event.endDate) {
      return startText;
    }

    return `${startText} → ${endText}`;
  }

  private parseDate(date: string): Date {
    const [year, month, day] = date.split('-').map(Number);

    return new Date(year, month - 1, day);
  }

  private isSameDate(
    first: Date,
    second: Date
  ): boolean {
    return first.getFullYear() === second.getFullYear()
      && first.getMonth() === second.getMonth()
      && first.getDate() === second.getDate();
  }
}