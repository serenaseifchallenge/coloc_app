import { Component, computed, input } from '@angular/core';
import { RoommateAvatar } from '../../../../shared/components/roommate-avatar/roommate-avatar';
import { LastMonthResults, PodiumEntry } from '../../models/points.model';

@Component({
  selector: 'app-last-month-podium',
  imports: [RoommateAvatar],
  templateUrl: './last-month-podium.html',
  styleUrl: './last-month-podium.css',
})
export class LastMonthPodium {
  readonly results = input.required<LastMonthResults>();

  readonly podiumOrder = [2, 1, 3] as const;

  readonly monthLabel = computed(() => {
    const [year, month] = this.results().monthStart.split('-').map(Number);
    return new Date(year, month - 1, 1).toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  });

  entryFor(rank: number): PodiumEntry | null {
    return this.results().podium.find((entry) => entry.rank === rank) ?? null;
  }
}