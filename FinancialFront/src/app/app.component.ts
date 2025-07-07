import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { LanguageService } from './services/language.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  constructor(
    private translate: TranslateService,
    private languageService: LanguageService
  ) {
    // Language setup is now handled by LanguageService
  }

  ngOnInit() {
    // Subscribe to language changes
    this.languageService.getCurrentLang().subscribe(lang => {
      // Save the language preference to localStorage
      localStorage.setItem('preferredLanguage', lang);
    });

    // Load saved language preference
    const savedLang = localStorage.getItem('preferredLanguage');
    if (savedLang) {
      this.languageService.setLanguage(savedLang);
    }
  }
}