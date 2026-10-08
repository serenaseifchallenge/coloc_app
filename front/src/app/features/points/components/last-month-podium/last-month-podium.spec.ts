import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LastMonthPodium } from './last-month-podium';

describe('LastMonthPodium', () => {
  let component: LastMonthPodium;
  let fixture: ComponentFixture<LastMonthPodium>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LastMonthPodium]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LastMonthPodium);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
