import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TaskNameManagerDialog } from './task-name-manager-dialog';

describe('TaskNameManagerDialog', () => {
  let component: TaskNameManagerDialog;
  let fixture: ComponentFixture<TaskNameManagerDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskNameManagerDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TaskNameManagerDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
