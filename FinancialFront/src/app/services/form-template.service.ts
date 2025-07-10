import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class FormTemplateService {
  private apiUrl = 'http://localhost:8081/api/form-templates';

  constructor(private http: HttpClient) { }

  // Method to save the template (authentication added)
  saveTemplate(template: any): Observable<any> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.post(this.apiUrl, template, { headers });
  }
}
