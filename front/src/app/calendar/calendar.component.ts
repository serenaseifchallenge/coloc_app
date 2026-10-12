import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BackdropClick } from '../shared/directives/backdrop-click';
import { CalendarService } from './calendar.service';
import {
  CalendarEvent,
  CalendarEventRequest
} from './calendar-event.model';
import { AuthService } from '../core/auth/auth.service';

type CalendarView = 'month' | 'week';

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [FormsModule, BackdropClick],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.css'
})
export class CalendarComponent implements OnInit {
  currentDate = new Date();
  selectedDate = new Date();

  view: CalendarView = 'month';

  showEventForm = false;
  editingEventId: number | null = null;

  loading = false;
  hasAccess = false;

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
    public calendarService: CalendarService,
    private authService: AuthService
  ) {}

  async ngOnInit(): Promise<void> {
    this.loading = true;

    try {
      const user = await this.authService.loadUser();

      if (!user) {
        this.calendarService.clearEvents();
        this.hasAccess = false;
        alert('Veuillez vous connecter pour accéder au calendrier.');
        return;
      }

      if (user.sharedHouseId === null) {
        this.calendarService.clearEvents();
        this.hasAccess = false;
        alert(
          'Vous devez rejoindre une colocation pour accéder au calendrier.'
        );
        return;
      }

      this.hasAccess = true;
      this.loadEvents();
    } finally {
      this.loading = false;
    }
  }

  private loadEvents(): void {
    this.calendarService.getEvents().subscribe({
      next: events => {
        console.log(
          'Événements récupérés depuis le backend :',
          events
        );
      },
      error: error => {
        console.error(
          'Erreur lors du chargement des événements :',
          error
        );

        alert('Impossible de charger les événements du calendrier.');
      }
    });
  }

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
    const sunday = new Date(date);

    sunday.setDate(date.getDate() - date.getDay());

    const days: Date[] = [];

    for (let i = 0; i < 7; i++) {
      const day = new Date(sunday);
      day.setDate(sunday.getDate() + i);
      days.push(day);
    }

    return days;
  }

  get selectedDayEvents(): CalendarEvent[] {
    return [...this.getEventsForDate(this.selectedDate)].sort((a, b) => {
      if (a.allDay && !b.allDay) {
        return -1;
      }

      if (!a.allDay && b.allDay) {
        return 1;
      }

      if (a.startTime === null) {
        return -1;
      }

      if (b.startTime === null) {
        return 1;
      }

      return a.startTime.localeCompare(b.startTime);
    });
  }

  getEventsForDate(date: Date): CalendarEvent[] {
    const dateString = this.formatDate(date);

    return this.calendarService.events().filter(event =>
      dateString >= event.startDate &&
      dateString <= event.endDate
    );
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
    if (!this.hasAccess) {
      alert('Vous devez appartenir à une colocation pour créer un événement.');
      return;
    }

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
    if (!this.hasAccess) {
      return;
    }

    this.editingEventId = event.id;

    this.newEvent = {
      title: event.title,
      startDate: event.startDate,
      endDate: event.endDate,
      allDay: event.allDay,
      startTime: event.startTime ?? '18:00',
      endTime: event.endTime ?? '19:00',
      description: event.description ?? ''
    };

    this.showEventForm = true;
  }

  closeEventForm(): void {
    this.showEventForm = false;
    this.editingEventId = null;
  }

  createOrUpdateEvent(): void {
    if (!this.hasAccess) {
      alert('Vous devez appartenir à une colocation pour continuer.');
      return;
    }

    if (!this.newEvent.title.trim()) {
      alert('Le titre de l’événement est obligatoire.');
      return;
    }

    if (!this.newEvent.startDate || !this.newEvent.endDate) {
      alert('Les dates de début et de fin sont obligatoires.');
      return;
    }

    if (this.newEvent.endDate < this.newEvent.startDate) {
      alert(
        'La date de fin doit être postérieure ou égale à la date de début.'
      );
      return;
    }

    if (
      !this.newEvent.allDay &&
      this.newEvent.endDate === this.newEvent.startDate &&
      this.newEvent.endTime <= this.newEvent.startTime
    ) {
      alert('L’heure de fin doit être postérieure à l’heure de début.');
      return;
    }

    const eventRequest: CalendarEventRequest = {
      title: this.newEvent.title.trim(),
      description: this.newEvent.description.trim(),
      startDate: this.newEvent.startDate,
      endDate: this.newEvent.endDate,
      allDay: this.newEvent.allDay,
      startTime: this.newEvent.allDay
        ? null
        : this.newEvent.startTime,
      endTime: this.newEvent.allDay
        ? null
        : this.newEvent.endTime
    };

    if (this.editingEventId === null) {
      this.calendarService.addEvent(eventRequest).subscribe({
        next: createdEvent => {
          console.log('Événement créé :', createdEvent);

          this.selectedDate = this.parseDate(createdEvent.startDate);
          this.currentDate = new Date(this.selectedDate);

          this.closeEventForm();
        },
        error: error => {
          console.error('Erreur lors de la création :', error);
          alert('Impossible de créer l’événement.');
        }
      });

      return;
    }

    this.calendarService
      .updateEvent(this.editingEventId, eventRequest)
      .subscribe({
        next: updatedEvent => {
          console.log('Événement modifié :', updatedEvent);

          this.selectedDate = this.parseDate(updatedEvent.startDate);
          this.currentDate = new Date(this.selectedDate);

          this.closeEventForm();
        },
        error: error => {
          console.error('Erreur lors de la modification :', error);
          alert('Impossible de modifier l’événement.');
        }
      });
  }

  deleteEvent(event: CalendarEvent): void {
    if (!this.hasAccess) {
      return;
    }

    const confirmation = confirm(
      `Voulez-vous supprimer l’événement "${event.title}" ?`
    );

    if (!confirmation) {
      return;
    }

    this.calendarService.deleteEvent(event.id).subscribe({
      next: () => {
        console.log('Événement supprimé :', event.id);
      },
      error: error => {
        console.error('Erreur lors de la suppression :', error);
        alert('Impossible de supprimer l’événement.');
      }
    });
  }

  isToday(date: Date): boolean {
    return this.isSameDate(date, new Date());
  }

  isSelected(date: Date): boolean {
    return this.isSameDate(date, this.selectedDate);
  }

  isCurrentMonth(date: Date): boolean {
    return date.getMonth() === this.currentDate.getMonth() &&
      date.getFullYear() === this.currentDate.getFullYear();
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

  formatTime(time: string | null): string {
    return time ? time.substring(0, 5) : '';
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

  private isSameDate(first: Date, second: Date): boolean {
    return first.getFullYear() === second.getFullYear() &&
      first.getMonth() === second.getMonth() &&
      first.getDate() === second.getDate();
  }
}