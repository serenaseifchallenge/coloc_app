import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NextTitleGoal } from './next-title-goal';

describe('NextTitleGoal', () => {
  let component: NextTitleGoal;
  let fixture: ComponentFixture<NextTitleGoal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NextTitleGoal]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NextTitleGoal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
