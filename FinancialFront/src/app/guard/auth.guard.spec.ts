import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { AuthGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { of } from 'rxjs';

class MockAuthService {
  // Mock the isAuthenticated method to simulate authentication state
  isAuthenticated() {
    return true;  // Simulate that the user is authenticated (true)
  }
}

describe('AuthGuard', () => {
  let authGuard: AuthGuard;
  let authService: MockAuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [
        AuthGuard,
        { provide: AuthService, useClass: MockAuthService }, // Use the mock AuthService
      ],
    });

    authGuard = TestBed.inject(AuthGuard);
    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  it('should be created', () => {
    expect(authGuard).toBeTruthy(); // Ensure the guard is created successfully
  });

  it('should allow access if authenticated', () => {
    spyOn(authService, 'isAuthenticated').and.returnValue(true); // Simulate authentication as true
    const result = authGuard.canActivate({} as any, {} as any); // Call the guard's canActivate method
    expect(result).toBe(true);  // Expect access to be allowed
  });

  it('should deny access if not authenticated', () => {
    spyOn(authService, 'isAuthenticated').and.returnValue(false); // Simulate authentication as false
    spyOn(router, 'navigate'); // Spy on the router to check if redirection happens

    const result = authGuard.canActivate({} as any, {} as any); // Call the guard's canActivate method
    expect(result).toBe(false);  // Expect access to be denied
    expect(router.navigate).toHaveBeenCalledWith(['/login']);  // Expect the router to redirect to the login page
  });
});
