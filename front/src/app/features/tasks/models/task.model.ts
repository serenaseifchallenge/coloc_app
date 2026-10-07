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

export interface CreateTaskRequest {
  name: string;
  deadline: string | null;
  assigneeId: number | null;
  points: number;
}

export type TaskAction = 'complete' | 'reopen' | 'delete';

export interface TaskActionRequest {
  action: TaskAction;
  task: Task;
}