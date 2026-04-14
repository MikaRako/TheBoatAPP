import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BoatService, Boat, BoatPage, BoatRequest } from './boat.service';
import { environment } from '../../../environments/environment';

describe('BoatService', () => {
  let service: BoatService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/boats`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [BoatService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BoatService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch a paginated list of boats with default params', () => {
    const mockPage: BoatPage = {
      content: [{ id: 1, name: 'Sea Breeze', description: 'Sailing boat', status: 'IN_PORT', type: 'SAILBOAT', createdAt: '2024-01-01' }],
      page: 0, size: 10, totalElements: 1, totalPages: 1, last: true,
    };

    service.getBoats().subscribe(page => {
      expect(page.totalElements).toBe(1);
      expect(page.content[0].name).toBe('Sea Breeze');
    });

    const req = httpMock.expectOne(r => r.url === apiUrl);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('10');
    req.flush(mockPage);
  });

  it('should pass search and pagination params', () => {
    service.getBoats('breeze', 1, 5).subscribe();
    const req = httpMock.expectOne(r => r.url === apiUrl);
    expect(req.request.params.get('search')).toBe('breeze');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('5');
    req.flush({ content: [], page: 1, size: 5, totalElements: 0, totalPages: 0, last: true });
  });

  it('should fetch a boat by id', () => {
    const mockBoat: Boat = { id: 1, name: 'Sea Breeze', description: 'Sailing boat', status: 'IN_PORT', type: 'SAILBOAT', createdAt: '2024-01-01' };

    service.getBoatById(1).subscribe(boat => {
      expect(boat.id).toBe(1);
      expect(boat.name).toBe('Sea Breeze');
    });

    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockBoat);
  });

  it('should send a POST request to create a boat', () => {
    const request: BoatRequest = { name: 'New Wave', description: 'Motor boat', status: 'UNDERWAY', type: 'YACHT' };
    const mockBoat: Boat = { id: 2, name: 'New Wave', description: 'Motor boat', status: 'UNDERWAY', type: 'YACHT', createdAt: '2024-01-02' };

    service.createBoat(request).subscribe(boat => {
      expect(boat.id).toBe(2);
      expect(boat.name).toBe('New Wave');
    });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockBoat);
  });

  it('should send a PUT request to update a boat', () => {
    const request: BoatRequest = { name: 'Renamed Boat', status: 'MAINTENANCE', type: 'FERRY' };
    const mockBoat: Boat = { id: 1, name: 'Renamed Boat', description: '', status: 'MAINTENANCE', type: 'FERRY', createdAt: '2024-01-01' };

    service.updateBoat(1, request).subscribe(boat => {
      expect(boat.name).toBe('Renamed Boat');
    });

    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush(mockBoat);
  });

  it('should send a DELETE request to remove a boat', () => {
    service.deleteBoat(1).subscribe(result => {
      expect(result).toBeNull();
    });

    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
