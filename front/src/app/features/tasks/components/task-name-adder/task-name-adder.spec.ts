import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TaskNameAdder } from './task-name-adder';

describe('TaskNameAdder', () => {
  let component: TaskNameAdder;
  let fixture: ComponentFixture<TaskNameAdder>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskNameAdder]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TaskNameAdder);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
