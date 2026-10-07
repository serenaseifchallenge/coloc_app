import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateTaskDialog } from './create-task-dialog';

describe('CreateTaskDialog', () => {
  let component: CreateTaskDialog;
  let fixture: ComponentFixture<CreateTaskDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateTaskDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateTaskDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
