import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FactoryFormComponent } from './factory-form.component';

describe('FactoryFormComponent', () => {
  let component: FactoryFormComponent;
  let fixture: ComponentFixture<FactoryFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FactoryFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(FactoryFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
