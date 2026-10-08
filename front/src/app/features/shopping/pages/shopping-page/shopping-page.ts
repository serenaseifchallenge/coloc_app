import { Component } from '@angular/core';
import { ShoppingList } from '../../components/shopping-list/shopping-list';

@Component({
  selector: 'app-shopping-page',
  imports: [ShoppingList],
  templateUrl: './shopping-page.html',
  styleUrl: './shopping-page.css',
})
export class ShoppingPage {
  
}