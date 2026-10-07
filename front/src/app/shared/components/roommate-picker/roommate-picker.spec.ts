import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RoommatePicker } from './roommate-picker';

describe('RoommatePicker', () => {
  let component: RoommatePicker;
  let fixture: ComponentFixture<RoommatePicker>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoommatePicker]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RoommatePicker);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
