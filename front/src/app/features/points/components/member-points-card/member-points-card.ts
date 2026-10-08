import { Component, computed, input } from '@angular/core';
import { RoommateAvatar } from '../../../../shared/components/roommate-avatar/roommate-avatar';
import { Roommate } from '../../../../shared/models/roommate.model';
import { titleFor } from '../../../../shared/utils/titles';

@Component({
  selector: 'app-member-points-card',
  imports: [RoommateAvatar],
  templateUrl: './member-points-card.html',
  styleUrl: './member-points-card.css',
})
export class MemberPointsCard {
  readonly roommate = input.required<Roommate>();

  readonly points = computed(() => Math.max(this.roommate().points ?? 0, 0));
  readonly title = computed(() => titleFor(this.points()));
}