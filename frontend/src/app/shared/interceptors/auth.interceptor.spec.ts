import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let oauthSpy: jasmine.SpyObj<OAuthService>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    oauthSpy = jasmine.createSpyObj('OAuthService', ['getAccessToken']);
    authSpy  = jasmine.createSpyObj('AuthService', ['login']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: OAuthService, useValue: oauthSpy },
        { provide: AuthService,  useValue: authSpy  },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  afterEach(() => httpMock.verify());

  it('should attach Bearer token to API requests', () => {
    oauthSpy.getAccessToken.and.returnValue('my-token');

    http.get('/api/boats').subscribe();

    const req = httpMock.expectOne('/api/boats');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-token');
    req.flush([]);
  });

  it('should not attach Authorization header when no token is available', () => {
    oauthSpy.getAccessToken.and.returnValue(null as any);

    http.get('/api/boats').subscribe();

    const req = httpMock.expectOne('/api/boats');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush([]);
  });

  it('should skip Authorization header for Keycloak realm URLs', () => {
    oauthSpy.getAccessToken.and.returnValue('my-token');

    http.get('http://localhost:8080/realms/boat-realm/token').subscribe();

    const req = httpMock.expectOne('http://localhost:8080/realms/boat-realm/token');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('should call authService.login() on a 401 response', () => {
    oauthSpy.getAccessToken.and.returnValue(null as any);

    http.get('/api/boats').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/boats');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authSpy.login).toHaveBeenCalled();
  });

  it('should navigate to /boats on a 403 response', () => {
    oauthSpy.getAccessToken.and.returnValue(null as any);

    http.get('/api/admin').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/admin');
    req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });

    expect(router.navigate).toHaveBeenCalledWith(['/boats']);
  });

  it('should propagate non-403 errors without redirecting', () => {
    oauthSpy.getAccessToken.and.returnValue(null as any);
    let errorStatus = 0;

    http.get('/api/boats').subscribe({ error: (e) => (errorStatus = e.status) });

    const req = httpMock.expectOne('/api/boats');
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

    expect(errorStatus).toBe(500);
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
