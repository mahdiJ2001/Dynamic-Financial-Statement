import { TestBed } from '@angular/core/testing';

import { FormTemplateService } from './form-template.service';

describe('FormTemplateService', () => {
  let service: FormTemplateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FormTemplateService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
