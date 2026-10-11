export interface CalendarEvent {
  id: number;
  title: string;
  description: string | null;
  startDate: string;
  endDate: string;
  allDay: boolean;
  startTime: string | null;
  endTime: string | null;
  creatorId: number;
  creatorName: string;
  creatorInitials: string;
  sharedHouseId: number;
}

export interface CalendarEventRequest {
  title: string;
  description: string | null;
  startDate: string;
  endDate: string;
  allDay: boolean;
  startTime: string | null;
  endTime: string | null;
  creatorId: number;
  sharedHouseId: number;
}