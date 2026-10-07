import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TaskNamePicker } from './task-name-picker';

describe('TaskNamePicker', () => {
  let component: TaskNamePicker;
  let fixture: ComponentFixture<TaskNamePicker>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskNamePicker]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TaskNamePicker);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
