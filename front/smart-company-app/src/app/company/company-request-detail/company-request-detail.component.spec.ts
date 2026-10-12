import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CompanyRequestDetailComponent } from './company-request-detail.component';

describe('CompanyRequestDetailComponent', () => {
  let component: CompanyRequestDetailComponent;
  let fixture: ComponentFixture<CompanyRequestDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompanyRequestDetailComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CompanyRequestDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
