import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type BoatStatus = 'UNDERWAY' | 'IN_PORT' | 'MAINTENANCE';
export type BoatType = 'SAILBOAT' | 'TRAWLER' | 'CARGO_SHIP' | 'YACHT' | 'FERRY';

export interface Boat {
  id: number;
  name: string;
  description: string;
  status: BoatStatus;
  type: BoatType;
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
  status: BoatStatus;
  type: BoatType;
}

@Injectable({ providedIn: 'root' })
export class BoatService {
  private readonly apiUrl = `${environment.apiUrl}/boats`;

  constructor(private http: HttpClient) {}

  getBoats(
    search = '',
    page = 0,
    size = 10,
    sortBy = 'createdAt',
    sortDir = 'desc',
    status: BoatStatus | '' = '',
    type: BoatType | '' = ''
  ): Observable<BoatPage> {
    let params = new HttpParams()
      .set('search', search)
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    if (status) params = params.set('status', status);
    if (type) params = params.set('type', type);
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
