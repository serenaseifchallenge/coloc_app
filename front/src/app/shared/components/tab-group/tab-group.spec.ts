import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TabGroup } from './tab-group';

describe('TabGroup', () => {
  let component: TabGroup;
  let fixture: ComponentFixture<TabGroup>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TabGroup]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TabGroup);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
