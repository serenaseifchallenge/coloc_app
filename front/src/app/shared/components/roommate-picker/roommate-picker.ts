import { Component, computed, input, output } from '@angular/core';
import { RoommateSummary } from '../../models/roommate-summary.model';
import { RoommateAvatar } from '../roommate-avatar/roommate-avatar';

@Component({
  selector: 'app-roommate-picker',
  imports: [RoommateAvatar],
  templateUrl: './roommate-picker.html',
  styleUrl: './roommate-picker.css',
  host: { role: 'group' },
})
export class RoommatePicker {
  readonly roommates = input.required<RoommateSummary[]>();
  readonly selectedId = input<number | null>(null);
  readonly selectedIdChange = output<number | null>();

  readonly selectedRoommate = computed(
    () => this.roommates().find((roommate) => roommate.id === this.selectedId()) ?? null,
  );

  toggle(roommateId: number): void {
    this.selectedIdChange.emit(roommateId === this.selectedId() ? null : roommateId);
  }
}