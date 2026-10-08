import { Component, computed, input } from '@angular/core';
import { TitleProgress } from '../../../../shared/utils/titles';

const RADIUS = 42;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

@Component({
  selector: 'app-next-title-goal',
  templateUrl: './next-title-goal.html',
  styleUrl: './next-title-goal.css',
})
export class NextTitleGoal {
  readonly progress = input.required<TitleProgress>();

  readonly radius = RADIUS;
  readonly circumference = CIRCUMFERENCE;
  readonly dashOffset = computed(() => CIRCUMFERENCE * (1 - this.progress().percent / 100));
}