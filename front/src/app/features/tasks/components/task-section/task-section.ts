import { Component, input, output } from '@angular/core';
import { Task, TaskActionRequest } from '../../models/task.model';
import { TaskStatus } from '../../utils/task-status';
import { TaskCard } from '../task-card/task-card';

@Component({
  selector: 'app-task-section',
  imports: [TaskCard],
  templateUrl: './task-section.html',
  styleUrl: './task-section.css',
})
export class TaskSection {
  readonly heading = input.required<string>();
  readonly status = input.required<TaskStatus>();
  readonly tasks = input.required<Task[]>();
  readonly showAssignee = input(true);
  readonly busyTaskIds = input<ReadonlySet<number>>(new Set());
  readonly taskActionRequested = output<TaskActionRequest>();
}