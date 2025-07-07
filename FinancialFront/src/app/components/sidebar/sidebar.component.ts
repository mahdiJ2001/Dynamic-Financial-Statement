import { Component, OnInit } from '@angular/core';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { NgbCollapseModule } from '@ng-bootstrap/ng-bootstrap';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { LanguageService } from '../../services/language.service';
import { TranslateService, TranslateModule } from '@ngx-translate/core';

declare interface RouteInfo {
  path: string;
  title: string;
  icon: string;
  class: string;
  iconColor?: string;
}

const ROUTE_KEYS: { path: string, titleKey: string, icon: string, class: string }[] = [
  { path: '/dashboard', titleKey: 'sidebar.dashboard', icon: 'fas fa-home', class: 'icon-danger' },
  { path: '/template-builder', titleKey: 'sidebar.createTemplate', icon: 'fas fa-pencil-alt', class: 'icon-danger' },
  { path: '/rule-builder', titleKey: 'sidebar.createRule', icon: 'fas fa-table', class: 'icon-danger' },
  { path: '/template-list', titleKey: 'sidebar.templateList', icon: 'fas fa-list', class: 'icon-danger' },
  { path: '/report-list', titleKey: 'sidebar.reportList', icon: 'fas fa-file-alt', class: 'icon-danger' },
  { path: '/notifications', titleKey: 'sidebar.notifications', icon: 'fas fa-bell', class: 'icon-danger' }
];


@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    NgbCollapseModule,
    TranslateModule
  ]
})
export class SidebarComponent implements OnInit {

  public menuItems: RouteInfo[] = [];
  public isCollapsed = true;
  public isSidenavHidden: boolean = false;
  public currentLang: string = 'fr';

  public currentHeader: { icon: string, title: string } = { icon: '', title: '' };

  constructor(
    private router: Router,
    private authService: AuthService,
    private languageService: LanguageService,
    private translate: TranslateService
  ) { }

  ngOnInit() {
    this.buildMenuItems();

    // Collapse sidebar on route change
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.isCollapsed = true;
        this.updateCurrentHeader(event.urlAfterRedirects);
      }
    });

    // Watch for language changes
    this.languageService.getCurrentLang().subscribe(lang => {
      this.currentLang = lang;
      this.translate.use(lang); // Ensure TranslateService uses this language
      this.buildMenuItems();
      this.updateCurrentHeader(this.router.url); // Update header when language changes
    });

    this.translate.onLangChange.subscribe(() => {
      this.buildMenuItems();
      this.updateCurrentHeader(this.router.url); // Update header on language switch
    });

    // Initialize header on component load
    this.updateCurrentHeader(this.router.url);
  }


  buildMenuItems() {
    const userRoles = this.authService.getUserRoles();
    this.menuItems = [];

    ROUTE_KEYS.forEach(item => {
      if (item.path === '/rule-builder' && userRoles.includes('USER')) {
        return; // Skip this item
      }

      this.translate.get(item.titleKey).subscribe(translatedTitle => {
        this.menuItems.push({
          path: item.path,
          title: translatedTitle,
          icon: item.icon,
          class: item.class
        });
      });
    });
  }

  toggleSidenav() {
    this.isSidenavHidden = !this.isSidenavHidden;
  }

  logout() {
    this.authService.logout();
  }

  isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  setLanguage(lang: string) {
    this.languageService.setLanguage(lang);
  }

  updateCurrentHeader(path: string) {
    const route = ROUTE_KEYS.find(r => path.startsWith(r.path));
    if (route) {
      this.translate.get(route.titleKey).subscribe(translatedTitle => {
        this.currentHeader = {
          icon: route.icon,
          title: translatedTitle
        };
      });
    } else {
      // Default header if no match (optional)
      this.translate.get('sidebar.dashboard').subscribe(translatedTitle => {
        this.currentHeader = {
          icon: 'fas fa-home',
          title: translatedTitle
        };
      });
    }
  }

}
