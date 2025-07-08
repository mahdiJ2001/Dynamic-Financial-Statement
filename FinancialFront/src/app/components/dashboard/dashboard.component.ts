import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { Chart, ChartData, ChartOptions, ChartType, PieController, ArcElement, Tooltip, Legend, LineController, LineElement, PointElement, Title, Filler, CategoryScale, LinearScale, BarController, BarElement } from 'chart.js';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { TemplateListService } from '../../services/template-list.service';
import { AuthService } from '../../services/auth.service';
import { ActivityLogDTO } from '../../models/activity-log.model';
import { ActivityLogService } from '../../services/activity-log.service';
import { BaseChartDirective } from 'ng2-charts';
import { Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';

Chart.register(
  CategoryScale,
  LinearScale,
  BarController,
  BarElement,
  ArcElement,
  Tooltip,
  Legend,
  LineController,
  LineElement,
  PointElement,
  Title,
  Filler
);


@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  encapsulation: ViewEncapsulation.None,
  imports: [CommonModule, BaseChartDirective, TranslateModule, FormsModule],
})
export class DashboardComponent implements OnInit {
  userName: string | null = null;

  templatesCount: number = 0;
  reportsCount: number = 0;
  dmnCount: number = 0;
  usersCount: number = 0;

  templatesPercentageChange: number = 0;
  reportsPercentageChange: number = 0;
  dmnPercentageChange: number = 0;
  usersPercentageChange: number = 0;

  horizontalBarChartLegend = true;
  horizontalBarChartLabels: string[] = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug'];

  activityLogs: any[] = [];
  paginatedLogs: any[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 10;
  totalPages: number = 1;
  loadingLogs = false;
  logsError = '';

  horizontalBarChartData: ChartData<'bar'> = {
    datasets: [
      {
        label: '', // Will be set dynamically
        data: [],
        backgroundColor: '#36a2eb',
        borderColor: '#36a2eb',
        borderWidth: 1,
        maxBarThickness: 15,
        barThickness: 15,
      },
    ],
  };

  horizontalBarChartOptions: ChartOptions = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top',
      },
      tooltip: {
        callbacks: {
          label: (tooltipItem) => `${tooltipItem.label}: ${tooltipItem.raw} templates`,
        },
      },
    },
    maintainAspectRatio: false,
    scales: {
      x: {
        beginAtZero: true,
        grid: {
          lineWidth: 0.5,
          color: '#ddd',
        },
      },
      y: {
        beginAtZero: true,
        ticks: {
          stepSize: 10,
          maxTicksLimit: 5,
          callback: (value) => `${value} `,
        },
        grid: {
          lineWidth: 0.5,
          color: '#ddd',
        },
      },
    },
    layout: {
      padding: {
        top: 10,
        right: 10,
        bottom: 10,
        left: 10,
      },
    },
  };

  horizontalBarChartType: ChartType = 'bar';

  lineChartLegend = true;

  lineChartData: ChartData<'line'> = {
    labels: ['01/03', '02/03', '03/03', '04/03', '05/03'],
    datasets: [{
      data: [2, 4, 6, 3, 5],
      label: '', // Will be set dynamically
      fill: false,
      borderColor: '#ff6384',
      tension: 0.1
    }]
  };


  lineChartOptions: ChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: this.lineChartLegend
      },
      tooltip: {
        callbacks: {
          label: (tooltipItem) => `${tooltipItem.label}: ${tooltipItem.raw}`
        }
      }
    },
    scales: {
      x: {
        title: { display: true, text: 'Période' }
      },
      y: {
        title: { display: true, text: 'Nombre de rapports' },
        beginAtZero: true
      }
    }
  };

  lineChartType: ChartType = 'line';
  lineChartLabels: string[] = [];

  selectedRange: 'month' | 'week' = 'month';

  private langChangeSub: Subscription | undefined;

  actionTypes = [
    { value: 'CREATE_TEMPLATE', label: 'dashboard.activityLogs.actionTypes.CREATE_TEMPLATE' },
    { value: 'SUBMIT_REPORT', label: 'dashboard.activityLogs.actionTypes.SUBMIT_REPORT' },
    { value: 'DELETE_TEMPLATE', label: 'dashboard.activityLogs.actionTypes.DELETE_TEMPLATE' },
    { value: 'VALIDATE_REPORT', label: 'dashboard.activityLogs.actionTypes.VALIDATE_REPORT' },
    { value: 'CREATE_VALIDATION_MODEL', label: 'dashboard.activityLogs.actionTypes.CREATE_VALIDATION_MODEL' },
    { value: 'REJECT_REPORT', label: 'dashboard.activityLogs.actionTypes.REJECT_REPORT' },
    { value: 'DELETE_VALIDATION_MODEL', label: 'dashboard.activityLogs.actionTypes.DELETE_VALIDATION_MODEL' }
  ];
  selectedActionType: string = 'ALL';

  constructor(private router: Router, private templateService: TemplateListService, private authService: AuthService, private translate: TranslateService, private activityLogService: ActivityLogService) { }

  ngOnInit(): void {
    this.loadUserName();
    this.loadStats();
    this.setChartLabels();
    this.loadActivityLogs();
    this.langChangeSub = this.translate.onLangChange.subscribe(() => {
      this.setChartLabels();
    });
    this.templateService.getAllFinancialStatements().subscribe(
      (statements: any[]) => {
        console.log('Financial statements loaded:', statements.length);
        this.processReportData(statements);
      },
      error => {
        console.error('Error loading financial statements:', error);
        if (error.status === 401) {
          console.warn('Authentication error, redirecting to login');
          localStorage.removeItem('token');
          this.router.navigate(['/login']);
        }
      }
    );
  }

  ngOnDestroy(): void {
    if (this.langChangeSub) {
      this.langChangeSub.unsubscribe();
    }
  }

  private loadUserName(): void {
    const token = this.authService.getToken();
    if (token) {
      const decoded: any = this.authService.decodeToken(token);

      this.userName = decoded?.username || 'Utilisateur';
    }
  }

  calculatePercentageChange(current: number, previous: number): number {
    if (previous === 0) return 0; // Avoid division by zero
    return ((current - previous) / previous) * 100;
  }


  goToTemplates(): void {
    this.router.navigate(['/template-list']);
  }

  goToTemplateBuilder(): void {
    this.router.navigate(['/template-builder']);
  }

  goToFinancialStatementsList() {
    this.router.navigate(['/report-list']);
  }


  loadStats(): void {
    // Load templates data
    this.templateService.getAllTemplates().subscribe((templates: any[]) => {
      this.templatesCount = templates.length;
      this.getPreviousMonthCount('templates').subscribe((previousMonthCount) => {
        this.templatesPercentageChange = this.calculatePercentageChange(this.templatesCount, previousMonthCount);
      });
      this.updateTemplateChartData(templates);
    });

    // Load reports data
    this.templateService.getAllFinancialStatements().subscribe((statements: any[]) => {
      this.reportsCount = statements.length;
      this.getPreviousMonthCount('reports').subscribe((previousMonthReports) => {
        this.reportsPercentageChange = this.calculatePercentageChange(this.reportsCount, previousMonthReports);
      });
    });

    // Load DMN rules data
    this.templateService.getAllDmnRules().subscribe((dmnRules: any[]) => {
      this.dmnCount = dmnRules.length;
      this.getPreviousMonthCount('dmn').subscribe((previousMonthDmn) => {
        this.dmnPercentageChange = this.calculatePercentageChange(this.dmnCount, previousMonthDmn);
      });
    });

    // Load users data
    this.authService.getAllUsers().subscribe((users: any[]) => {
      this.usersCount = users.length;
      this.getPreviousMonthCount('users').subscribe((previousMonthUsers) => {
        this.usersPercentageChange = this.calculatePercentageChange(this.usersCount, previousMonthUsers);
      });
    });
  }


  getPreviousMonthCount(type: 'templates' | 'reports' | 'dmn' | 'users'): Observable<number> {
    const now = new Date();
    const firstDayOfCurrentMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    const firstDayOfPreviousMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const lastDayOfPreviousMonth = new Date(firstDayOfCurrentMonth.getTime() - 1);

    switch (type) {
      case 'templates':
        return this.templateService.getAllTemplates().pipe(
          map((templates) => {
            return templates.filter((template: any) => {
              const creationDate = new Date(template.dateCreation);
              return creationDate >= firstDayOfPreviousMonth && creationDate <= lastDayOfPreviousMonth;
            }).length;
          })
        );
      case 'reports':
        return this.templateService.getAllFinancialStatements().pipe(
          map((reports) => {
            return reports.filter((report: any) => {
              const creationDate = new Date(report.createdAt);
              return creationDate >= firstDayOfPreviousMonth && creationDate <= lastDayOfPreviousMonth;
            }).length;
          })
        );
      case 'dmn':
        return this.templateService.getAllDmnRules().pipe(
          map((dmnRules) => {
            return dmnRules.filter((dmn: any) => {
              const creationDate = new Date(dmn.dateCreated);  // Adjust date property accordingly
              return creationDate >= firstDayOfPreviousMonth && creationDate <= lastDayOfPreviousMonth;
            }).length;
          })
        );
      case 'users':
        return this.authService.getAllUsers().pipe(
          map((users) => {
            return users.filter((user: any) => {
              const creationDate = new Date(user.createdAt);  // Adjust date property accordingly
              return creationDate >= firstDayOfPreviousMonth && creationDate <= lastDayOfPreviousMonth;
            }).length;
          })
        );
      default:
        return new Observable((observer) => {
          observer.next(0);
          observer.complete();
        });
    }
  }


  updateTemplateChartData(templates: any[]): void {
    const countsByMonth: { [key: string]: number } = {};

    templates.forEach(template => {
      const date = new Date(template.dateCreation);
      const monthLabel = this.horizontalBarChartLabels[date.getMonth()];
      countsByMonth[monthLabel] = (countsByMonth[monthLabel] || 0) + 1;
    });

    this.horizontalBarChartData = {
      datasets: [
        {
          label: this.translate.instant('dashboard.charts.templatesCreated.title'),
          data: this.horizontalBarChartLabels.map(month => countsByMonth[month] || 0),
          backgroundColor: '#ff5733',
          borderColor: '#ff5733',
          borderWidth: 1,
          maxBarThickness: 15,
          barThickness: 15
        }
      ]
    };
  }


  // Regroupement des rapports en fonction de la plage temporelle sélectionnée
  processReportData(statements: any[]): void {
    const countsByPeriod: { [key: string]: number } = {};

    statements.forEach(report => {
      const date = new Date(report.createdAt);
      let label = '';
      if (this.selectedRange === 'month') {
        // Regroupement par jour (format : dd/mm)
        label = `${date.getDate()}/${date.getMonth() + 1}`;
      } else if (this.selectedRange === 'week') {
        // Regroupement par semaine (approximation)
        const weekNumber = Math.ceil(date.getDate() / 7);
        label = `S${weekNumber}`;
      }
      countsByPeriod[label] = (countsByPeriod[label] || 0) + 1;
    });

    // Tri des labels chronologiquement
    let sortedLabels = Object.keys(countsByPeriod);
    if (this.selectedRange === 'month') {
      sortedLabels = sortedLabels.sort((a, b) => {
        const [dayA, monthA] = a.split('/').map(Number);
        const [dayB, monthB] = b.split('/').map(Number);
        const dateA = new Date(new Date().getFullYear(), monthA - 1, dayA);
        const dateB = new Date(new Date().getFullYear(), monthB - 1, dayB);
        return dateA.getTime() - dateB.getTime();
      });
    } else if (this.selectedRange === 'week') {
      sortedLabels = sortedLabels.sort((a, b) => {
        const weekA = Number(a.replace('S', ''));
        const weekB = Number(b.replace('S', ''));
        return weekA - weekB;
      });
    }

    this.lineChartLabels = sortedLabels;
    this.lineChartData = {
      datasets: [{
        data: sortedLabels.map(label => countsByPeriod[label]),
        label: this.translate.instant('dashboard.charts.reportsGenerated.title'),
        fill: false,
        borderColor: '#ff6384',
        tension: 0.1
      }]
    };
  }


  selectTimeRange(range: 'month' | 'week'): void {
    this.selectedRange = range;

    this.templateService.getAllFinancialStatements().subscribe((statements: any[]) => {
      this.processReportData(statements);
    });
  }

  setChartLabels(): void {
    if (this.horizontalBarChartData.datasets[0]) {
      this.horizontalBarChartData.datasets[0].label = this.translate.instant('dashboard.charts.templatesCreated.title');
    }
    if (this.lineChartData.datasets[0]) {
      this.lineChartData.datasets[0].label = this.translate.instant('dashboard.charts.reportsGenerated.title');
    }

    if ((window as any).Chart) {
      (window as any).Chart.helpers.each(Chart.instances, function (instance: any) {
        if (instance && instance.update) { instance.update(); }
      });
    }
  }


  loadActivityLogs(): void {
    this.loadingLogs = true;
    this.activityLogService.getAllLogs().subscribe({
      next: (logs) => {
        this.activityLogs = logs;
        this.updatePaginatedLogs();
        this.loadingLogs = false;
      },
      error: () => {
        this.logsError = 'Failed to load activity logs.';
        this.loadingLogs = false;
      }
    });
  }

  // Pagination methods
  updatePaginatedLogs(): void {
    let filtered = this.activityLogs;
    if (this.selectedActionType !== 'ALL') {
      filtered = this.activityLogs.filter(log => log.actionType === this.selectedActionType);
    }
    this.totalPages = Math.ceil(filtered.length / this.itemsPerPage) || 1;
    const start = (this.currentPage - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    this.paginatedLogs = filtered.slice(start, end);
  }

  getPages(): number[] {
    return Array(this.totalPages).fill(0).map((_, i) => i + 1);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePaginatedLogs();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.updatePaginatedLogs();
    }
  }

  prevPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updatePaginatedLogs();
    }
  }

  applyActionTypeFilter(): void {
    let filtered = this.activityLogs;
    if (this.selectedActionType !== 'ALL') {
      filtered = this.activityLogs.filter(log => log.actionType === this.selectedActionType);
    }
    this.currentPage = 1;
    this.totalPages = Math.ceil(filtered.length / this.itemsPerPage) || 1;
    this.paginatedLogs = filtered.slice(0, this.itemsPerPage);
  }
}
