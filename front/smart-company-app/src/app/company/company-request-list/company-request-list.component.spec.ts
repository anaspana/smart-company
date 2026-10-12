import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CompanyRequestListComponent } from './company-request-list.component';

describe('CompanyRequestListComponent', () => {
  let component: CompanyRequestListComponent;
  let fixture: ComponentFixture<CompanyRequestListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompanyRequestListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CompanyRequestListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
