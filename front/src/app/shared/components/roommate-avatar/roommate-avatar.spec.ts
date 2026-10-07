import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RoommateAvatar } from './roommate-avatar';

describe('RoommateAvatar', () => {
  let component: RoommateAvatar;
  let fixture: ComponentFixture<RoommateAvatar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoommateAvatar]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RoommateAvatar);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
