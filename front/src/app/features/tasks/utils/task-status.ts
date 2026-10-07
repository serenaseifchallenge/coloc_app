import { Task } from '../models/task.model';

export type TaskStatus = 'late' | 'upcoming' | 'no-date' | 'done';

const ONE_DAY_MS = 24 * 60 * 60 * 1000;

export function getTaskStatus(task: Task): TaskStatus {
  if (task.done) {
    return 'done';
  }
  if (!task.deadline) {
    return 'no-date';
  }
  return task.deadline < todayIso() ? 'late' : 'upcoming';
}

export function daysLate(deadline: string): number {
  return Math.round((Date.parse(todayIso()) - Date.parse(deadline)) / ONE_DAY_MS);
}

export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-');
  return `${day}/${month}/${year}`;
}

function todayIso(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}