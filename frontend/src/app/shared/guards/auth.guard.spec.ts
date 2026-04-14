import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let oauthSpy: jasmine.SpyObj<OAuthService>;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    oauthSpy = jasmine.createSpyObj('OAuthService', ['hasValidAccessToken']);
    authSpy  = jasmine.createSpyObj('AuthService', ['login']);

    TestBed.configureTestingModule({
      providers: [
        { provide: OAuthService, useValue: oauthSpy },
        { provide: AuthService,  useValue: authSpy  },
      ],
    });
  });

  const runGuard = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/boats' } as RouterStateSnapshot)
    );

  it('should allow navigation when a valid token exists', () => {
    oauthSpy.hasValidAccessToken.and.returnValue(true);
    expect(runGuard()).toBeTrue();
  });

  it('should block navigation and redirect to Keycloak when no valid token exists', () => {
    oauthSpy.hasValidAccessToken.and.returnValue(false);
    expect(runGuard()).toBeFalse();
    expect(authSpy.login).toHaveBeenCalledWith('/boats');
  });
});
