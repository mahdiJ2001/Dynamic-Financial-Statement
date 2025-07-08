import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) { }

  // auth.guard.ts

  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return false;
    }

    const expectedRoles = next.data['roles'] as string[];
    if (expectedRoles && expectedRoles.length > 0) {
      const userRoles = this.authService.getUserRoles();
      const hasAccess = expectedRoles.some(role => userRoles.includes(role));
      if (!hasAccess) {
        this.router.navigate(['/dashboard']); // or redirect to a 403 page
        return false;
      }
    }

    return true;
  }



}
