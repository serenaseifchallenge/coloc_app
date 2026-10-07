import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TaskSection } from './task-section';

describe('TaskSection', () => {
  let component: TaskSection;
  let fixture: ComponentFixture<TaskSection>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskSection]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TaskSection);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
