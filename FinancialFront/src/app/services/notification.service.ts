import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Notification } from '../models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  private apiUrl = 'http://localhost:8081/api/notifications';

  constructor(private http: HttpClient) { }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token ? token : ''}`
    });
  }

  getUnreadNotifications(): Observable<Notification[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<Notification[]>(`${this.apiUrl}/unread`, { headers });
  }

  markAllAsRead(): Observable<void> {
    const headers = this.getAuthHeaders();
    return this.http.put<void>(`${this.apiUrl}/mark-as-read`, {}, { headers });
  }
}
