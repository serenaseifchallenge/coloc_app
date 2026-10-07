import { Component, computed, input } from '@angular/core';
import { RoommateSummary } from '../../models/roommate-summary.model';

const AVATAR_COLORS = ['#6c63ff', '#c400e8', '#1e6b12', '#e8261e', '#e07a00', '#0077b6'];

@Component({
  selector: 'app-roommate-avatar',
  templateUrl: './roommate-avatar.html',
  styleUrl: './roommate-avatar.css',
  host: {
    '[style.background-color]': 'color()',
    '[class.small]': "size() === 'small'",
    '[class.large]': "size() === 'large'",
    '[attr.title]': 'fullName()',
  },
})
export class RoommateAvatar {
  readonly roommate = input.required<RoommateSummary>();
  readonly size = input<'small' | 'medium' | 'large'>('medium');

  readonly initials = computed(() => `${this.roommate().name[0]}${this.roommate().surname[0]}`.toUpperCase());
  readonly fullName = computed(() => `${this.roommate().name} ${this.roommate().surname}`);
  readonly color = computed(() => AVATAR_COLORS[this.roommate().id % AVATAR_COLORS.length]);
}