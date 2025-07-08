import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ErrorMessagesService {
  private errorApiUrl = 'http://localhost:8081/api/errors';

  constructor(private http: HttpClient) { }

  // Get error message with authorization token
  getErrorMessage(errorCode: string): Observable<{ mappedError: string }> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.get<{ mappedError: string }>(`${this.errorApiUrl}/${errorCode}`, { headers });
  }
}
