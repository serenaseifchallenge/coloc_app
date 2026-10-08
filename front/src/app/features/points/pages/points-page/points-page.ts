import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { SharedHouseService } from '../../../../core/shared-house/shared-house.service';
import { titleProgress } from '../../../../shared/utils/titles';
import { LastMonthPodium } from '../../components/last-month-podium/last-month-podium';
import { MemberPointsCard } from '../../components/member-points-card/member-points-card';
import { NextTitleGoal } from '../../components/next-title-goal/next-title-goal';
import { PointsService } from '../../services/points.service';

@Component({
  selector: 'app-points-page',
  imports: [LastMonthPodium, MemberPointsCard, NextTitleGoal],
  templateUrl: './points-page.html',
  styleUrl: './points-page.css',
})
export class PointsPage {
  private readonly auth = inject(AuthService);
  private readonly houses = inject(SharedHouseService);
  private readonly pointsService = inject(PointsService);

  protected readonly lastMonthResults = toSignal(
    this.pointsService.getLastMonthResults().pipe(catchError(() => of(null))),
  );

  protected readonly members = computed(() =>
    [...(this.houses.house()?.members ?? [])].sort((a, b) => (b.points ?? 0) - (a.points ?? 0)),
  );

  protected readonly myProgress = computed(() => {
    const myId = this.auth.user()?.id;
    const me = this.members().find((member) => member.id === myId);
    return titleProgress(me?.points);
  });

  constructor() {
    this.houses.load(true).catch(() => undefined);
  }
}