import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormTemplateBuilderComponent } from './form-template-builder.component';

describe('FormTemplateBuilderComponent', () => {
  let component: FormTemplateBuilderComponent;
  let fixture: ComponentFixture<FormTemplateBuilderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormTemplateBuilderComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FormTemplateBuilderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
