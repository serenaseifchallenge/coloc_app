import { RoommateSummary } from '../../../shared/models/roommate-summary.model';

export interface Task {
  id: number;
  name: string;
  description: string | null;
  deadline: string | null;
  completionDate: string | null;
  done: boolean;
  points: number;
  assignee: RoommateSummary | null;
}

export interface TaskQuery {
  done: boolean;
  assignedToMe: boolean;
}