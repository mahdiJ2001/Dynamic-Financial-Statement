import { TestBed } from '@angular/core/testing';
import { ErrorMessagesService } from './error-handler.service';

describe('ErrorMessagesService', () => {
  let service: ErrorMessagesService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ErrorMessagesService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
