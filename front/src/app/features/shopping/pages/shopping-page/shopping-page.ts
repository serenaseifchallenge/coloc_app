import { Component, computed, inject } from '@angular/core';
import { AuthService } from '../../../../core/auth/auth.service';
import { SharedHouseService } from '../../../../core/shared-house/shared-house.service';
import { RoommateAvatar } from '../../../../shared/components/roommate-avatar/roommate-avatar';
import { ShoppingList } from '../../components/shopping-list/shopping-list';

@Component({
  selector: 'app-shopping-page',
  imports: [ShoppingList, RoommateAvatar],
  templateUrl: './shopping-page.html',
  styleUrl: './shopping-page.css',
})
export class ShoppingPage {
  protected readonly auth = inject(AuthService);
  private readonly houses = inject(SharedHouseService);

  protected readonly sharedListHeading = computed(() => this.houses.house()?.name ?? 'Liste commune');
}