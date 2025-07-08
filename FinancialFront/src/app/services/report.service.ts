import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ReportService {

  private apiUrl = 'http://localhost:8081/api/reports';

  constructor(private http: HttpClient) { }


  generateFinancialReport(inputJson: any, designName: string): Observable<Blob> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });

    const params = { designName: designName };

    return this.http.post(this.apiUrl, inputJson, {
      headers: headers,
      params: params,
      responseType: 'blob'
    });
  }
}
