export interface CalendarEvent {
  id: number;

  title: string;
  description: string;

  startDate: string;
  endDate: string;

  allDay: boolean;

  startTime: string;
  endTime: string;

  creatorName: string;
  creatorInitials: string;
}