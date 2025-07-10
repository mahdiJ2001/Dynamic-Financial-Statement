import { Component, OnInit } from '@angular/core';
import { NotificationService } from '../../services/notification.service';
import { Notification } from '../../models/notification.model';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-notification',
    standalone: true,
    imports: [
        CommonModule,
        TranslateModule,
        FormsModule
    ],
    templateUrl: './notification.component.html',
    styleUrls: ['./notification.component.css']
})
export class NotificationComponent implements OnInit {

    notifications: Notification[] = [];
    filteredNotifications: Notification[] = [];
    selectedFilter: string = 'ALL';
    loading = false;
    error = '';

    constructor(private notificationService: NotificationService) { }

    ngOnInit(): void {
        this.loadNotifications();
    }

    loadNotifications(): void {
        this.loading = true;
        this.error = '';
        this.notificationService.getUnreadNotifications().subscribe({
            next: (data) => {
                this.notifications = data;
                this.applyFilter();
                this.loading = false;
            },
            error: () => {
                this.error = 'notifications.error';
                this.loading = false;
            }
        });
    }

    markAllRead(): void {
        this.notificationService.markAllAsRead().subscribe({
            next: () => {
                this.notifications = [];
                this.filteredNotifications = [];
            },
            error: () => {
                this.error = 'notifications.markFailed';
            }
        });
    }

    isValidated(key: string | undefined): boolean {
        if (!key) return false;
        return key.toLowerCase().includes('validated') || key.toLowerCase().includes('validé');
    }

    isRejected(key: string | undefined): boolean {
        if (!key) return false;
        return key.toLowerCase().includes('rejected') || key.toLowerCase().includes('rejeté');
    }

    filterStatus(type: string): void {
        this.selectedFilter = type;
        this.applyFilter();
    }

    public applyFilter(): void {
        if (this.selectedFilter === 'ALL') {
            this.filteredNotifications = [...this.notifications];
        } else if (this.selectedFilter === 'VALIDATED') {
            this.filteredNotifications = this.notifications.filter(n => this.isValidated(n.messageKey));
        } else if (this.selectedFilter === 'REJECTED') {
            this.filteredNotifications = this.notifications.filter(n => this.isRejected(n.messageKey));
        }
    }

}
