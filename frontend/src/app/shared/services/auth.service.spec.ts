import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { Subject } from 'rxjs';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let oauthSpy: jasmine.SpyObj<OAuthService>;
  const events$ = new Subject<any>();

  beforeEach(() => {
    oauthSpy = jasmine.createSpyObj('OAuthService', [
      'configure',
      'loadDiscoveryDocument',
      'hasValidAccessToken',
      'getAccessToken',
      'getIdentityClaims',
      'logOut',
      'initCodeFlow',
      'setupAutomaticSilentRefresh',
    ]);
    (oauthSpy as any).events = events$.asObservable();
    oauthSpy.hasValidAccessToken.and.returnValue(false);
    oauthSpy.getAccessToken.and.returnValue('mock-token');

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideRouter([]),
        { provide: OAuthService, useValue: oauthSpy },
      ],
    });

    service = TestBed.inject(AuthService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should return false for isAuthenticated by default', () => {
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('should return an empty string for userName by default', () => {
    expect(service.userName()).toBe('');
  });

  it('should return the access token from OAuthService', () => {
    expect(service.getAccessToken()).toBe('mock-token');
    expect(oauthSpy.getAccessToken).toHaveBeenCalled();
  });

  it('should return null when OAuthService has no token', () => {
    oauthSpy.getAccessToken.and.returnValue(null as any);
    expect(service.getAccessToken()).toBeNull();
  });

  it('should call initCodeFlow on login()', () => {
    service.login();
    expect(oauthSpy.initCodeFlow).toHaveBeenCalledTimes(1);
  });

  it('should call logOut on logout()', () => {
    service.logout();
    expect(oauthSpy.logOut).toHaveBeenCalledTimes(1);
  });
});
