import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layouts/main-layout.component';
import { AuthLayoutComponent } from './layouts/auth-layout.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { FormTemplateBuilderComponent } from './components/form-template-builder/form-template-builder.component';
import { TemplateListComponent } from './components/template-list/template-list.component';
import { ReportListComponent } from './components/report-list/report-list.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { OAuthCallbackComponent } from './components/oauth-callback/oauth-callback.component';
import { RuleBuilderComponent } from './components/rule-builder/rule-builder.component';
import { NotificationComponent } from './components/notification/notification.component';
import { AuthGuard } from './guard/auth.guard';

export const appRoutes: Routes = [

    { path: 'oauth-callback', component: OAuthCallbackComponent },

    {
        path: '',
        component: MainLayoutComponent,
        children: [
            { path: 'notifications', component: NotificationComponent, canActivate: [AuthGuard] },
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
            { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
            { path: 'template-builder', component: FormTemplateBuilderComponent, canActivate: [AuthGuard] },
            { path: 'template-list', component: TemplateListComponent, canActivate: [AuthGuard] },
            {
                path: 'rule-builder',
                component: RuleBuilderComponent,
                canActivate: [AuthGuard],
                data: { roles: ['ADMIN', 'EXPERT_TECHNIQUE'] }
            }
            ,
            { path: 'report-list', component: ReportListComponent, canActivate: [AuthGuard] },
        ],
    },

    {
        path: '',
        component: AuthLayoutComponent,
        children: [
            { path: 'login', component: LoginComponent },
            { path: 'register', component: RegisterComponent },
        ],
    },
];

export default appRoutes;