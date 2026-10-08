import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MemberPointsCard } from './member-points-card';

describe('MemberPointsCard', () => {
  let component: MemberPointsCard;
  let fixture: ComponentFixture<MemberPointsCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MemberPointsCard]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MemberPointsCard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
