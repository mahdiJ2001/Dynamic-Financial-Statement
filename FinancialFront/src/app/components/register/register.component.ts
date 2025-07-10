import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css'],
  standalone: true,
  imports: [FormsModule, CommonModule, TranslateModule],
})
export class RegisterComponent {
  username: string = '';
  email: string = ''; password: string = '';
  errorMessage: string = '';
  loading: boolean = false;
  currentLang: string = 'fr';

  constructor(
    private authService: AuthService,
    private router: Router,
    private languageService: LanguageService
  ) {
    this.languageService.getCurrentLang().subscribe(lang => {
      this.currentLang = lang;
    });
  }

  onRegister(): void {
    if (!this.username || !this.email || !this.password) {
      this.errorMessage = 'register.errors.allFieldsRequired';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService.register(this.email, this.username, this.password).subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: (error) => {
        console.error('Erreur lors de l\'inscription :', error);
        this.errorMessage = 'register.errors.registrationFailed';
        this.loading = false;
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  goToRegister(): void {
    this.router.navigate(['/register']);
  }

  switchLanguage(lang: string): void {
    this.languageService.setLanguage(lang);
  }
}
