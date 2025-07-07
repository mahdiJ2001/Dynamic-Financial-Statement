import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ActivityLogDTO } from '../models/activity-log.model';

@Injectable({
  providedIn: 'root',
})
export class ActivityLogService {
  private baseUrl = 'http://localhost:8081/api/activity-logs';

  constructor(private http: HttpClient) { }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    });
  }

  getAllLogs(): Observable<ActivityLogDTO[]> {
    return this.http.get<ActivityLogDTO[]>(this.baseUrl, {
      headers: this.getAuthHeaders(),
    });
  }
}
