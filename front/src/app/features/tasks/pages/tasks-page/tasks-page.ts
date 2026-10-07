import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subscription } from 'rxjs';
import { TabGroup } from '../../../../shared/components/tab-group/tab-group';
import { CreateTaskDialog } from '../../components/create-task-dialog/create-task-dialog';
import { TaskNameManagerDialog } from '../../components/task-name-manager-dialog/task-name-manager-dialog';
import { TaskSection } from '../../components/task-section/task-section';
import { TASK_TABS, TaskTabConfig } from '../../models/task-tab';
import { Task, TaskActionRequest } from '../../models/task.model';
import { TaskService } from '../../services/task.service';
import { getTaskStatus, TaskStatus } from '../../utils/task-status';

interface Feedback {
  message: string;
  type: 'success' | 'error';
}

const FEEDBACK_DURATION_MS = 4000;

@Component({
  selector: 'app-tasks-page',
  imports: [TabGroup, TaskSection, CreateTaskDialog, TaskNameManagerDialog],
  templateUrl: './tasks-page.html',
  styleUrl: './tasks-page.css',
})
export class TasksPage {
  private readonly taskService = inject(TaskService);
  private readonly destroyRef = inject(DestroyRef);
  private loadSubscription?: Subscription;
  private feedbackTimeout?: ReturnType<typeof setTimeout>;

  readonly tabs = TASK_TABS;
  readonly activeTab = signal<TaskTabConfig>(TASK_TABS[0]);
  readonly isCreateDialogOpen = signal(false);
  readonly createDialogInitialName = signal('');
  readonly isNameManagerOpen = signal(false);

  readonly tasks = signal<Task[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly busyTaskIds = signal<ReadonlySet<number>>(new Set());
  readonly feedback = signal<Feedback | null>(null);

  readonly showAssignee = computed(() => this.activeTab().value !== 'mine');
  readonly lateTasks = computed(() => this.tasksWithStatus('late'));
  readonly upcomingTasks = computed(() => this.tasksWithStatus('upcoming'));
  readonly noDateTasks = computed(() => this.tasksWithStatus('no-date'));

  constructor() {
    this.loadTasks();
    this.destroyRef.onDestroy(() => clearTimeout(this.feedbackTimeout));
  }

  selectTab(tab: TaskTabConfig): void {
    if (tab.value === this.activeTab().value) {
      return;
    }
    this.activeTab.set(tab);
    this.loadTasks();
  }

  openCreateDialog(initialName = ''): void {
    this.createDialogInitialName.set(initialName);
    this.isCreateDialogOpen.set(true);
  }

  onTaskCreated(): void {
    this.loadTasks();
  }

  onTaskAction({ action, task }: TaskActionRequest): void {
    switch (action) {
      case 'complete':
        this.runTaskAction(task, this.taskService.completeTask(task.id), (completedTask) =>
          `+${completedTask.points} points pour ${completedTask.assignee?.name} 🎉`,
        );
        break;
      case 'reopen':
        if (confirm(`Remettre « ${task.name} » à faire ? ${task.assignee?.name ?? 'Le colocataire'} perdra ${task.points} points.`)) {
          this.runTaskAction(task, this.taskService.reopenTask(task.id), () =>
            `« ${task.name} » est de nouveau à faire.`,
          );
        }
        break;
      case 'delete':
        if (confirm(`Supprimer « ${task.name} » ?`)) {
          this.runTaskAction(task, this.taskService.deleteTask(task.id), () =>
            `« ${task.name} » a été supprimée.`,
          );
        }
        break;
    }
  }

  private runTaskAction<T>(task: Task, request: Observable<T>, successMessage: (result: T) => string): void {
    this.setBusy(task.id, true);

    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (result) => {
        this.tasks.update((tasks) => tasks.filter((current) => current.id !== task.id));
        this.setBusy(task.id, false);
        this.showFeedback({ message: successMessage(result), type: 'success' });
      },
      error: (error: HttpErrorResponse) => {
        this.setBusy(task.id, false);
        if (error.status === 404 || error.status === 409) {
          this.showFeedback({
            message: `« ${task.name} » a été modifiée entre-temps : la liste a été mise à jour.`,
            type: 'error',
          });
          this.loadTasks();
        } else {
          this.showFeedback({ message: 'Action impossible pour le moment, réessaie.', type: 'error' });
        }
      },
    });
  }

  private setBusy(taskId: number, busy: boolean): void {
    this.busyTaskIds.update((ids) => {
      const nextIds = new Set(ids);
      if (busy) {
        nextIds.add(taskId);
      } else {
        nextIds.delete(taskId);
      }
      return nextIds;
    });
  }

  private showFeedback(feedback: Feedback): void {
    clearTimeout(this.feedbackTimeout);
    this.feedback.set(feedback);
    this.feedbackTimeout = setTimeout(() => this.feedback.set(null), FEEDBACK_DURATION_MS);
  }

  private loadTasks(): void {
    this.loadSubscription?.unsubscribe();
    this.loading.set(true);
    this.errorMessage.set(null);

    this.loadSubscription = this.taskService
      .getTasks(this.activeTab().query)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (tasks) => {
          this.tasks.set(tasks);
          this.loading.set(false);
        },
        error: () => {
          this.errorMessage.set('Impossible de charger les tâches.');
          this.loading.set(false);
        },
      });
  }

  private tasksWithStatus(status: TaskStatus): Task[] {
    return this.tasks().filter((task) => getTaskStatus(task) === status);
  }
}