import { Component, computed, input } from '@angular/core';
import { RoommateAvatar } from '../../../../shared/components/roommate-avatar/roommate-avatar';
import { Task } from '../../models/task.model';
import { daysLate, formatDate, getTaskStatus } from '../../utils/task-status';

@Component({
  selector: 'app-task-card',
  imports: [RoommateAvatar],
  templateUrl: './task-card.html',
  styleUrl: './task-card.css',
  host: { '[class]': 'status()' },
})
export class TaskCard {
  readonly task = input.required<Task>();
  readonly showAssignee = input(true);

  readonly status = computed(() => getTaskStatus(this.task()));

  readonly isDone = computed(() => this.status() === 'done');
  readonly avatarSize = computed(() => (this.isDone() ? 'small' : 'medium'));

  readonly subtitle = computed(() => {
    const deadline = this.task().deadline;
    if (!deadline || this.isDone()) {
      return null;
    }
    if (this.status() === 'late') {
      const days = daysLate(deadline);
      return `En retard de ${days} jour${days > 1 ? 's' : ''}`;
    }
    return formatDate(deadline);
  });
  
}