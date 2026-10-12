import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FactoryListComponent } from './factory-list.component';

describe('FactoryListComponent', () => {
  let component: FactoryListComponent;
  let fixture: ComponentFixture<FactoryListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FactoryListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(FactoryListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
