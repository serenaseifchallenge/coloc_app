import { TabOption } from '../../../shared/components/tab-group/tab-group';
import { TaskQuery } from './task.model';

export type TaskTab = 'to-do' | 'mine' | 'done';

export interface TaskTabConfig extends TabOption {
  value: TaskTab;
  query: TaskQuery;
  emptyMessage: string;
}

export const TASK_TABS: readonly TaskTabConfig[] = [
  {
    value: 'to-do',
    label: 'À faire',
    query: { done: false, assignedToMe: false },
    emptyMessage: 'Aucune tâche à faire 🎉',
  },
  {
    value: 'mine',
    label: 'Mes tâches',
    query: { done: false, assignedToMe: true },
    emptyMessage: "Aucune tâche ne t'est assignée.",
  },
  {
    value: 'done',
    label: 'Faites',
    query: { done: true, assignedToMe: false },
    emptyMessage: 'Aucune tâche terminée pour le moment.',
  },
];