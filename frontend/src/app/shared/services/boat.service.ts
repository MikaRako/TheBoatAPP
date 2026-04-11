import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Boat {
  id: number;
  name: string;
  description: string;
  createdAt: string;
}

export interface BoatPage {
  content: Boat[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface BoatRequest {
  name: string;
  description?: string;
}

@Injectable({ providedIn: 'root' })
export class BoatService {
  private readonly apiUrl = `${environment.apiUrl}/boats`;

  constructor(private http: HttpClient) {}

  getBoats(search = '', page = 0, size = 10, sortBy = 'createdAt', sortDir = 'desc'): Observable<BoatPage> {
    const params = new HttpParams()
      .set('search', search)
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    return this.http.get<BoatPage>(this.apiUrl, { params });
  }

  getBoatById(id: number): Observable<Boat> {
    return this.http.get<Boat>(`${this.apiUrl}/${id}`);
  }

  createBoat(request: BoatRequest): Observable<Boat> {
    return this.http.post<Boat>(this.apiUrl, request);
  }

  updateBoat(id: number, request: BoatRequest): Observable<Boat> {
    return this.http.put<Boat>(`${this.apiUrl}/${id}`, request);
  }

  deleteBoat(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
