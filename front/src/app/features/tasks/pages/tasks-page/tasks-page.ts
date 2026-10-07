import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { TabGroup } from '../../../../shared/components/tab-group/tab-group';
import { TaskSection } from '../../components/task-section/task-section';
import { TASK_TABS, TaskTabConfig } from '../../models/task-tab';
import { Task } from '../../models/task.model';
import { TaskService } from '../../services/task.service';
import { getTaskStatus, TaskStatus } from '../../utils/task-status';

@Component({
  selector: 'app-tasks-page',
  imports: [TabGroup, TaskSection],
  templateUrl: './tasks-page.html',
  styleUrl: './tasks-page.css',
})
export class TasksPage {
  private readonly taskService = inject(TaskService);
  private readonly destroyRef = inject(DestroyRef);
  private loadSubscription?: Subscription;

  readonly tabs = TASK_TABS;
  readonly activeTab = signal<TaskTabConfig>(TASK_TABS[0]);

  readonly tasks = signal<Task[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly showAssignee = computed(() => this.activeTab().value !== 'mine');
  readonly lateTasks = computed(() => this.tasksWithStatus('late'));
  readonly upcomingTasks = computed(() => this.tasksWithStatus('upcoming'));
  readonly noDateTasks = computed(() => this.tasksWithStatus('no-date'));

  constructor() {
    this.loadTasks();
  }

  selectTab(tab: TaskTabConfig): void {
    if (tab.value === this.activeTab().value) {
      return;
    }
    this.activeTab.set(tab);
    this.loadTasks();
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