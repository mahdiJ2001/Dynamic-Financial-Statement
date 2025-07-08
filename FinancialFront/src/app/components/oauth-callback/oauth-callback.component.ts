// src/app/components/auth/oauth-callback.component.ts
import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-oauth-callback',
  template: '<div class="loading">Processing authentication, please wait...</div>',
  styles: ['.loading { display: flex; justify-content: center; align-items: center; height: 100vh; }']
})
export class OAuthCallbackComponent implements OnInit {
  constructor(
    private router: Router,
    private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    console.log('OAuth callback component initialized');

    // Extract token from URL parameters
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');

    // Extract token from cookies as fallback
    const tokenFromCookie = this.getCookie('auth_token');

    if (token || tokenFromCookie) {
      // Store token in localStorage
      localStorage.setItem('token', token || tokenFromCookie || '');
      console.log('Token stored in localStorage');

      // Navigate to dashboard
      this.router.navigate(['/dashboard']);
    } else {
      console.error('No token found in redirect');
      this.router.navigate(['/login'], {
        queryParams: { error: 'auth_failed' }
      });
    }
  }

  // Helper method to get cookie by name
  private getCookie(name: string): string | null {
    const cookieArr = document.cookie.split(';');

    for (let i = 0; i < cookieArr.length; i++) {
      const cookiePair = cookieArr[i].split('=');
      const cookieName = cookiePair[0].trim();

      if (cookieName === name) {
        return decodeURIComponent(cookiePair[1]);
      }
    }

    return null;
  }
}